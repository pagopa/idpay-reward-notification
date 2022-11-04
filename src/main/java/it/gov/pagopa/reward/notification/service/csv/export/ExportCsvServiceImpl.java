package it.gov.pagopa.reward.notification.service.csv.export;

import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.service.csv.export.retrieve.Initiative2ExportRetrieverService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
public class ExportCsvServiceImpl implements ExportCsvService {

    private final Initiative2ExportRetrieverService initiative2ExportRetrieverService;
    private final ExportInitiativeRewardsService exportInitiativeRewardsService;

    public ExportCsvServiceImpl(Initiative2ExportRetrieverService initiative2ExportRetrieverService, ExportInitiativeRewardsService exportInitiativeRewardsService) {
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

        Flux<RewardOrganizationExport> singleInitiativeExport =
                initiative2ExportRetrieverService.retrieve()
                        .flatMapMany(exportInitiativeRewardsService::performExport);

        // repeat until not more initiatives
        return singleInitiativeExport
                .expand(x -> singleInitiativeExport)
                .doFinally(x -> log.info("[PERFORMANCE_LOG][REWARD_NOTIFICATION_EXPORT_CSV] Reward notification export completed in {}ms", System.currentTimeMillis() - startTime));
    }
}
