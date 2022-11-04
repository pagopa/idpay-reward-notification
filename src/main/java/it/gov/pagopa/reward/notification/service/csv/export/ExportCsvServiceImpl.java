package it.gov.pagopa.reward.notification.service.csv.export;

import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class ExportCsvServiceImpl implements ExportCsvService {

    private final Initiative2ExportRetrieverService initiative2ExportRetrieverService;

    public ExportCsvServiceImpl(Initiative2ExportRetrieverService initiative2ExportRetrieverService) {
        this.initiative2ExportRetrieverService = initiative2ExportRetrieverService;
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

        long[] initiativeExportStartTime = new long[]{0L};

        Mono<RewardOrganizationExport> singleInitiativeExport =
                initiative2ExportRetrieverService.retrieve()
                        .doOnNext(export -> {
                            log.info("[REWARD_NOTIFICATION_EXPORT_CSV] Starting export of reward notification related to initiative {}", export.getInitiativeId());
                            initiativeExportStartTime[0] = System.currentTimeMillis();
                        })

                        .doOnNext(export -> log.info("[PERFORMANCE_LOG][REWARD_NOTIFICATION_EXPORT_CSV] Completed export of reward notification related to initiative: {}ms, initiative {}", System.currentTimeMillis() - initiativeExportStartTime[0], export.getInitiativeId()));

        // repeat until not more initiatives
        return singleInitiativeExport
                .expand(x -> singleInitiativeExport)
                .doFinally(x -> log.info("[PERFORMANCE_LOG][REWARD_NOTIFICATION_EXPORT_CSV] Reward notification export completed in {}ms", System.currentTimeMillis() - startTime));
    }
}
