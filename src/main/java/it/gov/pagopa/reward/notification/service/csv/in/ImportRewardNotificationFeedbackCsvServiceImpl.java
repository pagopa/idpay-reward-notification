package it.gov.pagopa.reward.notification.service.csv.in;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationImportCsvDto;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.service.csv.in.utils.ImportElaborationCounters;
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
import java.util.Comparator;

@Slf4j
@Service
public class ImportRewardNotificationFeedbackCsvServiceImpl implements ImportRewardNotificationFeedbackCsvService {

    private final char csvSeparator;
    private final Integer parallelism;

    private final RewardNotificationFeedbackHandlerService rewardNotificationFeedbackHandlerService;

    private final HeaderColumnNameStrategy<RewardNotificationImportCsvDto> mappingStrategy;

    public ImportRewardNotificationFeedbackCsvServiceImpl(
            @Value("${app.csv.import.separator}") char csvSeparator,
            @Value("${app.csv.import.parallelism}") Integer parallelism,

            RewardNotificationFeedbackHandlerService rewardNotificationFeedbackHandlerService) {
        this.csvSeparator = csvSeparator;
        this.parallelism = parallelism;
        this.rewardNotificationFeedbackHandlerService = rewardNotificationFeedbackHandlerService;

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

        int[] rowNumber = new int[]{1};
        return Flux.fromStream(csvReader.stream())
                .doOnNext(r -> r.setRowNumber(rowNumber[0]++))

                .parallel(parallelism)
                .runOn(Schedulers.boundedElastic())

                .flatMap(rewardNotificationFeedbackHandlerService::evaluate)
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
                .map(c -> updateImportRequest(c, importRequest));
    }

    private CsvToBean<RewardNotificationImportCsvDto> buildCsvReader(Reader reader) {
        return new CsvToBeanBuilder<RewardNotificationImportCsvDto>(reader)
                .withType(RewardNotificationImportCsvDto.class)
                .withMappingStrategy(mappingStrategy)
                .withSeparator(csvSeparator)
                .build();
    }

    private RewardOrganizationImport updateImportRequest(ImportElaborationCounters counter, RewardOrganizationImport importRequest) {
        importRequest.setRewardsResulted(counter.getRewardsResulted());
        importRequest.setRewardsResultedError(counter.getRewardsResultedError());
        importRequest.setRewardsResultedOk(counter.getRewardsResultedOk());
        importRequest.setRewardsResultedOkError(counter.getRewardsResultedOkError());

        long successfulProcess = importRequest.getRewardsResulted() - counter.getRewardsResultedError();
        long successfulOkOutcomes = counter.getRewardsResultedOk() - counter.getRewardsResultedOkError();

        importRequest.setPercentageResulted(calcPercentage(successfulProcess, importRequest.getRewardsResulted()));
        importRequest.setPercentageResultedOk(calcPercentage(counter.getRewardsResultedOk(), importRequest.getRewardsResulted()));
        importRequest.setPercentageResultedOkElab(calcPercentage(successfulOkOutcomes, successfulProcess));

        counter.getErrors().sort(Comparator.comparing(RewardOrganizationImport.RewardOrganizationImportError::getRow));
        importRequest.setErrors(counter.getErrors());

        return importRequest;
    }

    private long calcPercentage(long value, long total) {
        return (long) ((((double) value) / total) * 100_00); // storing into a 2 decimal percentage, multiplied by 100 in order to keep it an integer
    }
}
