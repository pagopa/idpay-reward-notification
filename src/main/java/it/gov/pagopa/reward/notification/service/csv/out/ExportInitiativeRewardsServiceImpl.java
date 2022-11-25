package it.gov.pagopa.reward.notification.service.csv.out;

import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationExportCsvDto;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationExportsRepository;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.csv.out.mapper.RewardNotification2ExportCsvService;
import it.gov.pagopa.reward.notification.service.csv.out.retrieve.Initiative2ExportRetrieverService;
import it.gov.pagopa.reward.notification.service.csv.out.writer.ExportCsvFinalizeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class ExportInitiativeRewardsServiceImpl implements ExportInitiativeRewardsService {

    private final int csvMaxRows;

    private final RewardsNotificationRepository rewardsNotificationRepository;
    private final RewardNotification2ExportCsvService reward2CsvLineService;
    private final Initiative2ExportRetrieverService initiative2ExportRetrieverService;
    private final RewardOrganizationExportsRepository exportsRepository;
    private final ExportCsvFinalizeService csvWriterService;

    private final Scheduler splitScheduler;

    public ExportInitiativeRewardsServiceImpl(
            @Value("${app.csv.export.split-size}") int csvMaxRows,
            RewardsNotificationRepository rewardsNotificationRepository, RewardNotification2ExportCsvService rewardNotification2ExportCsvService, Initiative2ExportRetrieverService initiative2ExportRetrieverService, RewardOrganizationExportsRepository exportsRepository, ExportCsvFinalizeService csvWriterService) {
        this.csvMaxRows = csvMaxRows;
        this.rewardsNotificationRepository = rewardsNotificationRepository;
        this.reward2CsvLineService = rewardNotification2ExportCsvService;
        this.initiative2ExportRetrieverService = initiative2ExportRetrieverService;
        this.exportsRepository = exportsRepository;
        this.csvWriterService = csvWriterService;

        splitScheduler = Schedulers.newSingle("exportSplitProcessor");
    }

    @Override
    public Flux<RewardOrganizationExport> performExport(RewardOrganizationExport export, boolean isStuckExport) {
        log.info("[REWARD_NOTIFICATION_EXPORT_CSV]{} Starting export of reward notification related to initiative {} into {}"
                , isStuckExport ? "[STUCK_EXECUTION]" : ""
                , export.getInitiativeId()
                , export.getId());
        long startTime = System.currentTimeMillis();

        AtomicInteger splitNumber = new AtomicInteger(0);

        //         1. search rewards 2 notify
        return retrieveRewards2Notify(export, isStuckExport)
                // 2. build CSV line
                .flatMap(reward2CsvLineService::apply)
                // 3. wait to fill split or the end of the rewards
                .buffer(csvMaxRows)
                // 3.1. if no rows has been correctly transformed into csvLines
                .switchIfEmpty(Mono.defer(()->deleteExportRequestWhenEmpty(export, splitNumber)))
                // 4. write CSV for each split and finalize the export
                .flatMap(csvLines -> writeAndFinalizeSplit(export, startTime, splitNumber, csvLines));
    }

    private Flux<RewardsNotification> retrieveRewards2Notify(RewardOrganizationExport export, boolean isStuckExport) {
        //         1. let's search as first if there are rewards already related to the current export
        return rewardsNotificationRepository.findExportRewards(export.getId())
                .switchIfEmpty(Mono.defer(() -> {
                    if(isStuckExport){
                        log.info("[REWARD_NOTIFICATION_EXPORT_CSV][STUCK_EXECUTION] Stuck execution has no previous records {}", export.getId());
                    } else {
                        log.debug("[REWARD_NOTIFICATION_EXPORT_CSV] Execution has no previous records {}", export.getId());
                    }
                    return Mono.empty();
                }))
                .doOnComplete(() -> log.info("[REWARD_NOTIFICATION_EXPORT_CSV]{} execution of previous records completed {}"
                        , isStuckExport? "[STUCK_EXECUTION] stuck" : ""
                        , export.getId()))
                // 2. let's search new rewards to notify not related to any exports
                .concatWith(
                        rewardsNotificationRepository.findRewards2Notify(export.getInitiativeId(), export.getNotificationDate())
                );
    }

    private Mono<List<RewardNotificationExportCsvDto>> deleteExportRequestWhenEmpty(RewardOrganizationExport emptyExport, AtomicInteger splitNumber) {
        int split = splitNumber.get();
        log.warn("[REWARD_NOTIFICATION_EXPORT_CSV] no rewards were successfully transformed into csv line for export {} and split {}", emptyExport.getId(), split);
        if(split==0){
            return exportsRepository.delete(emptyExport).then(Mono.empty());
        } else {
            return Mono.empty();
        }
    }

    private Mono<RewardOrganizationExport> writeAndFinalizeSplit(RewardOrganizationExport export, long startTime, AtomicInteger splitNumber, List<RewardNotificationExportCsvDto> csvLines) {
        Mono<RewardOrganizationExport> exportMono;
        int n = splitNumber.getAndIncrement();
        if (n == 0) {
            exportMono = Mono.just(export);
        } else {
            exportMono = initiative2ExportRetrieverService.reserveNextSplitExport(export, n)
                    // processing furthermore split once at time
                    .publishOn(Schedulers.single(splitScheduler));
        }
        return exportMono
                .flatMap(exp -> csvWriterService.writeCsvAndFinalize(csvLines, exp))
                .doOnNext(exp -> log.info("[PERFORMANCE_LOG][REWARD_NOTIFICATION_EXPORT_CSV] Completed export of reward notification related to initiative: from the beginning of the process: {}ms, initiative {}, fileName {}, split number {}", System.currentTimeMillis() - startTime, export.getInitiativeId(), exp.getFilePath(), n));
    }

}
