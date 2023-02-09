package it.gov.pagopa.reward.notification.service.csv.out;

import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.service.csv.out.retrieve.Initiative2ExportRetrieverService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class ExportRewardNotificationCsvServiceImpl implements ExportRewardNotificationCsvService {

    private final Initiative2ExportRetrieverService initiative2ExportRetrieverService;
    private final ExportInitiativeRewardsService exportInitiativeRewardsService;

    public ExportRewardNotificationCsvServiceImpl(Initiative2ExportRetrieverService initiative2ExportRetrieverService, ExportInitiativeRewardsService exportInitiativeRewardsService) {
        this.initiative2ExportRetrieverService = initiative2ExportRetrieverService;
        this.exportInitiativeRewardsService = exportInitiativeRewardsService;
    }

    @Scheduled(cron = "${app.csv.export.schedule}")
    void schedule() {
        log.debug("[REWARD_NOTIFICATION_EXPORT_CSV][SCHEDULE] Starting schedule to export reward notifications");
        this.execute()
                .subscribe(x -> log.debug("[REWARD_NOTIFICATION_EXPORT_CSV][SCHEDULE] Completed schedule to export reward notifications"));
    }

    @Override
    public Flux<RewardOrganizationExport> execute() {
        log.info("[REWARD_NOTIFICATION_EXPORT_CSV] Starting reward notifications export to CSV");
        long startTime = System.currentTimeMillis();

        Mono<RewardOrganizationExport> retrieveNewInitiativeExport =
                initiative2ExportRetrieverService.retrieve();

        Mono<RewardOrganizationExport> retrieveStuckInitiativeExportsThenNew =
                initiative2ExportRetrieverService.retrieveStuckExecution()
                        .switchIfEmpty(retrieveNewInitiativeExport);

        // repeat until not more initiatives
        return exportInitiative(retrieveStuckInitiativeExportsThenNew)
                .expand(x -> exportInitiative((
                        isStuckExecution(x)
                                ? retrieveStuckInitiativeExportsThenNew
                                : retrieveNewInitiativeExport
                )))
                .doFinally(x -> log.info("[PERFORMANCE_LOG] [REWARD_NOTIFICATION_EXPORT_CSV] Time occurred to perform business logic: {} ms", System.currentTimeMillis() - startTime));
    }

    private Flux<RewardOrganizationExport> exportInitiative(Mono<RewardOrganizationExport> exportRetriever) {
        return exportRetriever
                .flatMapMany(exp -> exportInitiativeRewardsService.performExport(exp, isStuckExecution(exp)));
    }

    private static boolean isStuckExecution(RewardOrganizationExport x) {
        return x.getNotificationDate().isBefore(x.getExportDate());
    }
}
