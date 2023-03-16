package it.gov.pagopa.reward.notification.service.feedback;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.reward.notification.dto.StorageEventDto;
import it.gov.pagopa.reward.notification.dto.mapper.StorageEvent2OrganizationImportMapper;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationImportStatus;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationImportsRepository;
import it.gov.pagopa.reward.notification.service.BaseKafkaBlockingPartitionConsumer;
import it.gov.pagopa.reward.notification.service.ErrorNotifierService;
import it.gov.pagopa.reward.notification.service.LockService;
import it.gov.pagopa.reward.notification.service.csv.in.ImportRewardNotificationFeedbackCsvService;
import it.gov.pagopa.reward.notification.service.email.EmailNotificationService;
import it.gov.pagopa.reward.notification.service.feedback.retrieve.FeedbackCsvRetrieverService;
import it.gov.pagopa.reward.notification.utils.PerformanceLogger;
import it.gov.pagopa.reward.notification.utils.RewardFeedbackConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@Service
@Slf4j
public class RewardNotificationFeedbackMediatorServiceImpl extends BaseKafkaBlockingPartitionConsumer<List<StorageEventDto>, List<RewardOrganizationImport>> implements RewardNotificationFeedbackMediatorService {
    private final StorageEvent2OrganizationImportMapper mapper;
    private final RewardOrganizationImportsRepository importsRepository;
    private final FeedbackCsvRetrieverService csvRetrieverService;
    private final ImportRewardNotificationFeedbackCsvService importRewardNotificationFeedbackCsvService;
    private final ErrorNotifierService errorNotifierService;
    private final EmailNotificationService emailNotificationService;
    private final String importEmailSubject;
    private final String importEmailTemplateName;

    private final Duration commitDelay;

    private final ObjectReader objectReader;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters constructor alert
    public RewardNotificationFeedbackMediatorServiceImpl(
            @Value("${spring.application.name}") String applicationName,
            @Value("${spring.cloud.stream.kafka.bindings.rewardTrxConsumer-in-0.consumer.ackTime}") long commitMillis,

            StorageEvent2OrganizationImportMapper mapper,

            LockService lockService,
            RewardOrganizationImportsRepository importsRepository,
            FeedbackCsvRetrieverService csvRetrieverService,
            ImportRewardNotificationFeedbackCsvService importRewardNotificationFeedbackCsvService,
            ErrorNotifierService errorNotifierService, EmailNotificationService emailNotificationService,
            @Value("${app.email-notification.imports.subject}") String importEmailSubject,
            @Value("${app.email-notification.imports.template-name}") String importEmailTemplateName,
            ObjectMapper objectMapper) {
        super(applicationName, lockService);
        this.mapper = mapper;
        this.importsRepository = importsRepository;
        this.csvRetrieverService = csvRetrieverService;
        this.importRewardNotificationFeedbackCsvService = importRewardNotificationFeedbackCsvService;

        this.errorNotifierService = errorNotifierService;
        this.commitDelay = Duration.ofMillis(commitMillis);
        this.emailNotificationService = emailNotificationService;
        this.importEmailSubject = importEmailSubject;
        this.importEmailTemplateName = importEmailTemplateName;

        this.objectReader = objectMapper.readerFor(new TypeReference<List<StorageEventDto>>() {
        });
    }

    @Override
    protected Duration getCommitDelay() {
        return commitDelay;
    }

    @Override
    protected void subscribeAfterCommits(Flux<List<List<RewardOrganizationImport>>> afterCommits2subscribe) {
        afterCommits2subscribe.subscribe(p -> log.info("[REWARD_NOTIFICATION_FEEDBACK] Processed offsets committed successfully"));
    }

    @Override
    protected void notifyError(Message<String> message, Throwable e) {
        errorNotifierService.notifyOrganizationFeedbackUpload(message, "[REWARD_NOTIFICATION_FEEDBACK] An error occurred handling organization feedback upload event", true, e);
    }

    @Override
    protected ObjectReader getObjectReader() {
        return objectReader;
    }

    @Override
    protected Consumer<Throwable> onDeserializationError(Message<String> message) {
        return e -> errorNotifierService.notifyOrganizationFeedbackUpload(message, "[REWARD_NOTIFICATION_FEEDBACK] Unexpected JSON", true, e);
    }

    @Override
    protected String getFlowName() {
        return "REWARD_NOTIFICATION_FEEDBACK";
    }

    @Override
    protected Mono<List<RewardOrganizationImport>> execute(List<StorageEventDto> payload, Message<String> message, Map<String, Object> ctx) {
        return Flux.fromIterable(payload)
                // consider just organization feedback upload events
                .filter(this::isOrganizationFeedbackUploadEvent)
                .doOnNext(i -> log.info("[REWARD_NOTIFICATION_FEEDBACK] Processing import request: {}", i.getSubject()))
                .map(mapper)
                // trace import request
                .flatMap(importsRepository::createIfNotExistsOrReturnEmpty)
                // retrieve and elaborate csv
                .flatMap(this::retrieveAndElaborateCsv)
                // finalize import request state and store it
                .flatMap(this::finalizeImportRequest)
                .flatMap(this::sendEmail)
                .doOnNext(i -> log.info("[REWARD_NOTIFICATION_FEEDBACK] Import request processing results stored: {}", i.getFilePath()))
                .collectList();
    }

    private Mono<RewardOrganizationImport> sendEmail(RewardOrganizationImport i) {
        return emailNotificationService.send(i, importEmailSubject, importEmailTemplateName);
    }

    private static final Pattern rewardOrganizationInputFilePathPattern = Pattern.compile("^%s[^/]+/[^/]+/import/[^/]*.zip$".formatted(RewardFeedbackConstants.AZURE_STORAGE_SUBJECT_PREFIX));

    private boolean isOrganizationFeedbackUploadEvent(StorageEventDto storageEventDto) {
        return
                // is upload event
                RewardFeedbackConstants.AZURE_STORAGE_EVENT_TYPE_BLOB_CREATED.equals(storageEventDto.getEventType())
                        // is at the expected path
                        && rewardOrganizationInputFilePathPattern.matcher(storageEventDto.getSubject()).matches();
    }

    private Mono<RewardOrganizationImport> retrieveAndElaborateCsv(RewardOrganizationImport importRequest) {
        log.info("[REWARD_NOTIFICATION_FEEDBACK] New request recognized, retrieving and processing it: {}", importRequest.getFilePath());
        return PerformanceLogger.logTimingOnNext(
                        "REWARD_NOTIFICATION_FEEDBACK_DOWNLOAD",
                        csvRetrieverService.retrieveCsv(importRequest),
                        i -> "downloaded feedback file %s on initiative %s".formatted(importRequest.getFilePath(), importRequest.getInitiativeId())
                )
                .flatMap(p -> importRewardNotificationFeedbackCsvService.evaluate(p, importRequest))
                .switchIfEmpty(Mono.fromSupplier(() -> {
                    log.error("[REWARD_NOTIFICATION_FEEDBACK] Cannot retrieve csv from filePath: {}", importRequest.getFilePath());
                    return importRequest;
                }))

                // catch errors
                .onErrorResume(e -> {
                    log.error("[REWARD_NOTIFICATION_FEEDBACK] Something gone wrong while elaborating import request {}", importRequest.getFilePath(), e);
                    importRequest.setStatus(RewardOrganizationImportStatus.ERROR);
                    importRequest.getErrors().add(new RewardOrganizationImport.RewardOrganizationImportError(RewardFeedbackConstants.ImportFileErrors.GENERIC_ERROR));
                    return Mono.just(importRequest);
                });
    }

    private Mono<RewardOrganizationImport> finalizeImportRequest(RewardOrganizationImport importRequest) {
        importRequest.setElabDate(LocalDateTime.now());
        importRequest.setStatus(transcodeStatus(importRequest));
        importRequest.setErrorsSize(importRequest.getErrors().size());

        log.info("[REWARD_NOTIFICATION_FEEDBACK] Import request process completed {} and resulted into status {}, read:{}, rowErrors:{}, errorsSize:{}",
                importRequest.getFilePath(), importRequest.getStatus(), importRequest.getRewardsResulted(), importRequest.getRewardsResultedError(), importRequest.getErrorsSize());

        // errors potentially could be a big list, printing only when DEBUG
        if (importRequest.getErrorsSize() > 0) {
            log.debug("[REWARD_NOTIFICATION_FEEDBACK] Import request completed with errors {}: {}", importRequest.getFilePath(), importRequest.getErrors());
        }
        return importsRepository.save(importRequest);
    }

    private static RewardOrganizationImportStatus transcodeStatus(RewardOrganizationImport importRequest) {
        RewardOrganizationImportStatus status;
        if (importRequest.getRewardsResulted() == 0) {
            if (importRequest.getErrors().isEmpty()) {
                importRequest.getErrors().add(
                        new RewardOrganizationImport.RewardOrganizationImportError(RewardFeedbackConstants.ImportFileErrors.NO_ROWS));
            }
            status = RewardOrganizationImportStatus.ERROR;
        } else if (!RewardOrganizationImportStatus.IN_PROGRESS.equals(importRequest.getStatus())) {
            status = importRequest.getStatus();
        } else if (importRequest.getRewardsResultedError() > 0) {
            status = RewardOrganizationImportStatus.WARN;
        } else {
            status = RewardOrganizationImportStatus.COMPLETE;
        }

        return status;
    }
}
