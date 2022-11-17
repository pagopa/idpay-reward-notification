package it.gov.pagopa.reward.notification.service.csv.in;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.reward.notification.dto.StorageEventDto;
import it.gov.pagopa.reward.notification.dto.mapper.StorageEvent2OrganizationImportMapper;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.service.BaseKafkaBlockingPartitionConsumer;
import it.gov.pagopa.reward.notification.service.ErrorNotifierService;
import it.gov.pagopa.reward.notification.service.LockService;
import it.gov.pagopa.reward.notification.utils.RewardFeedbackConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@Service
@Slf4j
public class RewardNotificationFeedbackMediatorServiceImpl extends BaseKafkaBlockingPartitionConsumer<List<StorageEventDto>, List<RewardOrganizationImport>> implements RewardNotificationFeedbackMediatorService {

    private final StorageEvent2OrganizationImportMapper mapper;
    private final ErrorNotifierService errorNotifierService;

    private final Duration commitDelay;

    private final ObjectReader objectReader;

    public RewardNotificationFeedbackMediatorServiceImpl(
            @Value("${spring.application.name}") String applicationName,
            @Value("${spring.cloud.stream.kafka.bindings.rewardTrxConsumer-in-0.consumer.ackTime}") long commitMillis,

            StorageEvent2OrganizationImportMapper mapper,

            LockService lockService, ErrorNotifierService errorNotifierService, ObjectMapper objectMapper) {
        super(applicationName, lockService);
        this.mapper = mapper;

        this.errorNotifierService = errorNotifierService;
        this.commitDelay = Duration.ofMillis(commitMillis);

        this.objectReader = objectMapper.readerFor(new TypeReference<List<StorageEventDto>>() {
        });
    }

    @Override
    protected Duration getCommitDelay() {
        return commitDelay;
    }

    @Override
    protected void subscribeAfterCommits(Flux<List<List<RewardOrganizationImport>>> afterCommits2subscribe) {
        afterCommits2subscribe.subscribe(p -> log.debug("[REWARD_NOTIFICATION_FEEDBACK] Processed offsets committed successfully"));
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
                .map(mapper)
                // TODO Check if exist or store
                // TODO downloadFile and decompress
                // TODO elaborate file
                .collectList();
    }

    private static final Pattern rewardOrganizationInputFilePathPattern = Pattern.compile("^%s/[^/]+/[^/]+/.*.zip$".formatted(RewardFeedbackConstants.AZURE_STORAGE_SUBJECT_PREFIX));
    private boolean isOrganizationFeedbackUploadEvent(StorageEventDto storageEventDto) {
        return
                // is upload event
                RewardFeedbackConstants.AZURE_STORAGE_EVENT_TYPE_BLOB_CREATED.equals(storageEventDto.getEventType())
                        // is at the expected path
                        && rewardOrganizationInputFilePathPattern.matcher(storageEventDto.getSubject()).matches();
    }
}
