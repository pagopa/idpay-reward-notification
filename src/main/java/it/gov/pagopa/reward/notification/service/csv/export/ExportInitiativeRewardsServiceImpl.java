package it.gov.pagopa.reward.notification.service.csv.export;

import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.csv.export.mapper.RewardNotification2ExportCsvService;
import it.gov.pagopa.reward.notification.service.csv.export.writer.ExportCsvFinalizeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayDeque;
import java.util.Deque;

@Slf4j
@Service
public class ExportInitiativeRewardsServiceImpl implements ExportInitiativeRewardsService {

    private final int csvMaxRows;

    private final RewardsNotificationRepository rewardsNotificationRepository;
    private final RewardNotification2ExportCsvService reward2CsvLineService;
    private final ExportCsvFinalizeService csvWriterService;

    public ExportInitiativeRewardsServiceImpl(
            @Value("${app.csv.export.split-size}") int csvMaxRows,
            RewardsNotificationRepository rewardsNotificationRepository, RewardNotification2ExportCsvService rewardNotification2ExportCsvService, ExportCsvFinalizeService csvWriterService) {
        this.csvMaxRows = csvMaxRows;
        this.rewardsNotificationRepository = rewardsNotificationRepository;
        this.reward2CsvLineService = rewardNotification2ExportCsvService;
        this.csvWriterService = csvWriterService;
    }

    @Override
    public Flux<RewardOrganizationExport> performExport(RewardOrganizationExport export) {
        log.info("[REWARD_NOTIFICATION_EXPORT_CSV] Starting export of reward notification related to initiative {}", export.getInitiativeId());
        long startTime = System.currentTimeMillis();

        Deque<RewardOrganizationExport> splits = new ArrayDeque<>();
        splits.add(export);

        return rewardsNotificationRepository.findRewards2Notify(export.getInitiativeId())
                .flatMap(reward2CsvLineService::apply)
                .buffer(csvMaxRows)
                // TODO create splits
                .flatMap(csvLines -> csvWriterService.writeCsvAndFinalize(csvLines, export))

                .doOnNext(exp -> log.info("[PERFORMANCE_LOG][REWARD_NOTIFICATION_EXPORT_CSV] Completed export of reward notification related to initiative: {}ms, initiative {}, fileName {}", System.currentTimeMillis() - startTime, export.getInitiativeId(), exp.getFilePath()));
    }

}
