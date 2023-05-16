package it.gov.pagopa.reward.notification.service.csv.out;

import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.service.csv.out.retrieve.Initiative2ExportRetrieverService;
import it.gov.pagopa.reward.notification.utils.ExportCsvConstants;
import it.gov.pagopa.reward.notification.utils.PerformanceLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        this.execute(LocalDate.now())
                .subscribe(x -> log.debug("[REWARD_NOTIFICATION_EXPORT_CSV][SCHEDULE] Completed schedule to export reward notifications"));
    }

    @Override
    public Flux<List<RewardOrganizationExport>> execute(LocalDate notificationDateToSearch) {
        log.info("[REWARD_NOTIFICATION_EXPORT_CSV] Starting reward notifications export to CSV");
        Mono<RewardOrganizationExport> retrieveNewInitiativeExport =
                initiative2ExportRetrieverService.retrieve(notificationDateToSearch);

        Mono<RewardOrganizationExport> retrieveStuckInitiativeExportsThenNew =
                initiative2ExportRetrieverService.retrieveStuckExecution()
                        .switchIfEmpty(retrieveNewInitiativeExport);

        Set<String> exportedInitiativeIds = new HashSet<>();

        // repeat until not more initiatives
        return PerformanceLogger.logTimingFinally(
                "REWARD_NOTIFICATION_EXPORT_CSV",
                exportInitiative(retrieveStuckInitiativeExportsThenNew)
                        // expand in order to repeat until an export has been reserved to be evaluated
                        .expand(x -> {
                            if(x.isEmpty()){
                                return Flux.empty();
                            } else {
                                RewardOrganizationExport firstSplit = x.get(0);
                                return exportInitiative((
                                                isStuckExecution(firstSplit)
                                                        ? retrieveStuckInitiativeExportsThenNew
                                                        : retrieveNewInitiativeExport
                                        ).contextWrite(ctx -> {
                                            exportedInitiativeIds.add(firstSplit.getInitiativeId());
                                            return Context.of(ExportCsvConstants.CTX_KEY_EXPORTED_INITIATIVE_IDS, exportedInitiativeIds);
                                        })
                                );
                            }
                        }),
                null);
    }

    private Mono<List<RewardOrganizationExport>> exportInitiative(Mono<RewardOrganizationExport> exportRetriever) {
        return PerformanceLogger.logTimingOnNext(
                        "REWARD_NOTIFICATION_LOCATE_INITIATIVE",
                        exportRetriever,
                        e -> "starting reward notification export on initiative %s into %s".formatted(e.getInitiativeId(), e.getFilePath())
                )
                .flatMapMany(exp -> exportInitiativeRewardsService.performExport(exp, isStuckExecution(exp)))
                .collectList();
    }

    private static boolean isStuckExecution(RewardOrganizationExport x) {
        return x.getNotificationDate().isBefore(x.getExportDate());
    }
}
