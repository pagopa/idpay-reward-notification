package it.gov.pagopa.reward.notification.service.csv.in;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationImportCsvDto;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.service.csv.in.retrieve.RewardNotificationExportFeedbackRetrieverService;
import it.gov.pagopa.reward.notification.service.csv.in.utils.ImportElaborationCounters;
import it.gov.pagopa.reward.notification.utils.PerformanceLogger;
import it.gov.pagopa.reward.notification.utils.Utils;
import it.gov.pagopa.reward.notification.utils.csv.HeaderColumnNameStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
    private final Integer parallelism;

    private final RewardNotificationFeedbackHandlerService rewardNotificationFeedbackHandlerService;
    private final RewardNotificationExportFeedbackRetrieverService exportFeedbackRetrieverService;

    private final HeaderColumnNameStrategy<RewardNotificationImportCsvDto> mappingStrategy;

    public ImportRewardNotificationFeedbackCsvServiceImpl(
            @Value("${app.csv.import.separator}") char csvSeparator,
            @Value("${app.csv.import.parallelism}") Integer parallelism,

            RewardNotificationFeedbackHandlerService rewardNotificationFeedbackHandlerService, RewardNotificationExportFeedbackRetrieverService exportFeedbackRetrieverService) {
        this.csvSeparator = csvSeparator;
        this.parallelism = parallelism;
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
                .doOnNext(r -> r.setRowNumber(rowNumber[0]++))

                .parallel(parallelism)
                .runOn(Schedulers.boundedElastic())

                .flatMap(line -> PerformanceLogger.logTimingOnNext(
                        "FEEDBACK_FILE_LINE_EVALUATION",
                        rewardNotificationFeedbackHandlerService.evaluate(line, importRequest, exportCache),
                        x -> "importFile %s, line %s".formatted(importRequest.getFilePath(), line.getRowNumber())))
                .map(ImportElaborationCounters::fromElaborationResult)

                .reduce(ImportElaborationCounters::add)
                .doFinally(x -> {
                    try {
                        reader.close();
                        Files.deleteIfExists(csv);
                    } catch (IOException e) {
                        log.error("[REWARD_NOTIFICATION_FEEDBACK] Cannot close local csv {}", csv, e);
                    }
                })
                .map(c -> updateImportRequest(c, importRequest))
                .flatMap(i -> exportFeedbackRetrieverService.updateExportStatus(i.getExportIds())
                        .then(Mono.just(i)));
    }

    private CsvToBean<RewardNotificationImportCsvDto> buildCsvReader(Reader reader) {
        return new CsvToBeanBuilder<RewardNotificationImportCsvDto>(reader)
                .withType(RewardNotificationImportCsvDto.class)
                .withMappingStrategy(mappingStrategy)
                .withSeparator(csvSeparator)
                .withFieldAsNull(CSVReaderNullFieldIndicator.BOTH)
                .build();
    }

    private RewardOrganizationImport updateImportRequest(ImportElaborationCounters counter, RewardOrganizationImport importRequest) {
        log.debug("[REWARD_NOTIFICATION_FEEDBACK] updating importRequest {} with counters {}", importRequest.getFilePath(), counter);

        importRequest.setExportIds(new ArrayList<>(counter.getExportIds()));
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
