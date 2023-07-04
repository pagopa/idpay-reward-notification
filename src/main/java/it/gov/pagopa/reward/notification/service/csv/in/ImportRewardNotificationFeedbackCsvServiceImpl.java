package it.gov.pagopa.reward.notification.service.csv.in;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.exceptionhandler.ExceptionHandlerQueue;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
import it.gov.pagopa.common.reactive.utils.PerformanceLogger;
import it.gov.pagopa.common.utils.csv.HeaderColumnNameStrategy;
import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationImportCsvDto;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.service.csv.in.retrieve.RewardNotificationExportFeedbackRetrieverService;
import it.gov.pagopa.reward.notification.service.csv.in.utils.ImportElaborationCounters;
import it.gov.pagopa.reward.notification.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Slf4j
@Service
public class ImportRewardNotificationFeedbackCsvServiceImpl implements ImportRewardNotificationFeedbackCsvService {

    private final char csvSeparator;

    private final RewardNotificationFeedbackHandlerService rewardNotificationFeedbackHandlerService;
    private final RewardNotificationExportFeedbackRetrieverService exportFeedbackRetrieverService;

    private final HeaderColumnNameStrategy<RewardNotificationImportCsvDto> mappingStrategy;

    public ImportRewardNotificationFeedbackCsvServiceImpl(
            @Value("${app.csv.import.separator}") char csvSeparator,

            RewardNotificationFeedbackHandlerService rewardNotificationFeedbackHandlerService, RewardNotificationExportFeedbackRetrieverService exportFeedbackRetrieverService) {
        this.csvSeparator = csvSeparator;
        this.rewardNotificationFeedbackHandlerService = rewardNotificationFeedbackHandlerService;
        this.exportFeedbackRetrieverService = exportFeedbackRetrieverService;

        this.mappingStrategy = new HeaderColumnNameStrategy<>(RewardNotificationImportCsvDto.class);
    }

    @Override
    public Mono<RewardOrganizationImport> evaluate(Path csv, RewardOrganizationImport importRequest) {
        log.info("[REWARD_NOTIFICATION_FEEDBACK] Processing csv file: {}", csv);
        Reader reader;
        CsvToBean<RewardNotificationImportCsvDto> csvReader;
        try {
            reader = Files.newBufferedReader(csv);
            csvReader = buildCsvReader(reader);
        } catch (IOException e) {
            throw new IllegalStateException("[REWARD_NOTIFICATION_FEEDBACK] Cannot read csv: %s".formatted(csv), e);
        }

        Map<String, RewardOrganizationExport> exportCache = new ConcurrentHashMap<>();

        int[] rowNumber = new int[]{1};
        return Flux.fromStream(csvReader.stream())
                .doOnEach(r -> {
                    int n = rowNumber[0]++;
                    if (SignalType.ON_NEXT.equals(r.getType()) && r.get() != null) {
                        r.get().setRowNumber(n);
                    }
                })

                .flatMap(line -> PerformanceLogger.logTimingOnNext(
                        "FEEDBACK_FILE_LINE_EVALUATION",
                        rewardNotificationFeedbackHandlerService.evaluate(line, importRequest, exportCache),
                        x -> "importFile %s, line %s".formatted(importRequest.getFilePath(), line.getRowNumber())))
                .map(ImportElaborationCounters::fromElaborationResult)

                .reduce(ImportElaborationCounters::add)
                .doOnNext(c -> ImportElaborationCounters.updateWithException(c, csvReader.getCapturedExceptions()))

                .doFinally(x -> {
                    try {
                        reader.close();
                        Files.deleteIfExists(csv);
                    } catch (IOException e) {
                        log.error("[REWARD_NOTIFICATION_FEEDBACK] Cannot close local csv {}", csv, e);
                    }
                })
                .flatMap(counters -> Flux.fromIterable(counters.getExportDeltas().values())
                        .flatMap(exportFeedbackRetrieverService::updateCounters)
                        .collectList()
                        .flatMapMany(x -> exportFeedbackRetrieverService.updateExportStatus(counters.getExportDeltas().keySet()))
                        .then(Mono.just(counters))
                )
                .map(counters -> updateImportRequest(counters, importRequest));
    }

    private CsvToBean<RewardNotificationImportCsvDto> buildCsvReader(Reader reader) {
        return new CsvToBeanBuilder<RewardNotificationImportCsvDto>(reader)
                .withType(RewardNotificationImportCsvDto.class)
                .withMappingStrategy(mappingStrategy)
                .withSeparator(csvSeparator)
                .withFieldAsNull(CSVReaderNullFieldIndicator.BOTH)
                .withExceptionHandler(new ExceptionHandlerQueue())
                .build();
    }

    private RewardOrganizationImport updateImportRequest(ImportElaborationCounters counter, RewardOrganizationImport importRequest) {
        log.info("[REWARD_NOTIFICATION_FEEDBACK] updating importRequest {} with counters {}", importRequest.getFilePath(), counter);

        importRequest.setExportIds(new ArrayList<>(counter.getExportDeltas().keySet()));
        importRequest.getExportIds().sort(Comparator.comparing(Function.identity()));

        importRequest.setRewardsResulted(counter.getRewardsResulted());
        importRequest.setRewardsResultedError(counter.getRewardsResultedError());
        importRequest.setRewardsResultedOk(counter.getRewardsResultedOk());
        importRequest.setRewardsResultedOkError(counter.getRewardsResultedOkError());

        long successfulProcess = importRequest.getRewardsResulted() - counter.getRewardsResultedError();
        long successfulOkOutcomes = counter.getRewardsResultedOk() - counter.getRewardsResultedOkError();

        importRequest.setPercentageResulted(Utils.calcPercentage(successfulProcess, importRequest.getRewardsResulted()));
        importRequest.setPercentageResultedOk(Utils.calcPercentage(counter.getRewardsResultedOk(), importRequest.getRewardsResulted()));
        importRequest.setPercentageResultedOkElab(Utils.calcPercentage(successfulOkOutcomes, successfulProcess));

        counter.getErrors().sort(Comparator.comparing(RewardOrganizationImport.RewardOrganizationImportError::getRow));
        importRequest.setErrors(counter.getErrors());

        return importRequest;
    }
}
