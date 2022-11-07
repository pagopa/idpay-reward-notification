package it.gov.pagopa.reward.notification.service.csv.export;

import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.csv.export.mapper.RewardNotification2ExportCsvService;
import it.gov.pagopa.reward.notification.service.csv.export.writer.ExportCsvFinalizeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    public Flux<RewardOrganizationExport> performExport(RewardOrganizationExport export, boolean isStuckExport) {
        log.info("[REWARD_NOTIFICATION_EXPORT_CSV]{} Starting export of reward notification related to initiative {} into {}"
                , isStuckExport ? "[STUCK_EXECUTION]" : ""
                , export.getInitiativeId()
                , export.getId());
        long startTime = System.currentTimeMillis();

        Deque<RewardOrganizationExport> splits = new ArrayDeque<>();
        splits.add(export);

        //         1. search rewards 2 notify
        return retrieveRewards2Notify(export, isStuckExport)
                // 2. build CSV line
                .flatMap(reward2CsvLineService::apply)
                // 3. wait to fill split or the end of the rewards
                .buffer(csvMaxRows)
                // TODO create splits
                // 4. write CSV for each split and finalize the export
                .flatMap(csvLines -> csvWriterService.writeCsvAndFinalize(csvLines, export))

                .doOnNext(exp -> log.info("[PERFORMANCE_LOG][REWARD_NOTIFICATION_EXPORT_CSV] Completed export of reward notification related to initiative: {}ms, initiative {}, fileName {}", System.currentTimeMillis() - startTime, export.getInitiativeId(), exp.getFilePath()));
    }

    private Flux<RewardsNotification> retrieveRewards2Notify(RewardOrganizationExport export, boolean isStuckExport) {
        return (isStuckExport
                // if stuck export, retrieve as first export related rewards
                ? rewardsNotificationRepository.findExportRewards(export.getId())
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("[REWARD_NOTIFICATION_EXPORT_CSV][STUCK_EXECUTION] Stuck execution has no records {}", export.getId());
                    return Mono.empty();
                }))
                .doOnComplete(() -> log.info("[REWARD_NOTIFICATION_EXPORT_CSV][STUCK_EXECUTION] Stuck execution records extraction completed"))

                : Flux.<RewardsNotification>empty()
        )
                .concatWith(
                        rewardsNotificationRepository.findRewards2Notify(export.getInitiativeId(), export.getNotificationDate())
                );
    }

}
