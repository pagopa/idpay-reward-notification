package it.gov.pagopa.reward.notification.service.csv.export;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class ExportCsvServiceImpl implements ExportCsvService {

    private final Initiative2ExportRetrieverService initiative2ExportRetrieverService;

    public ExportCsvServiceImpl(Initiative2ExportRetrieverService initiative2ExportRetrieverService) {
        this.initiative2ExportRetrieverService = initiative2ExportRetrieverService;
    }

    @Scheduled(cron = "${app.csv.export.schedule}")
    void schedule(){
        log.debug("[REWARD_NOTIFICATION_EXPORT_CSV][SCHEDULE] Starting schedule to export reward notifications");
        this.execute()
                .subscribe(x -> log.debug("[REWARD_NOTIFICATION_EXPORT_CSV][SCHEDULE] Completed schedule to export reward notifications"));
    }

    @Override
    public Mono<?> execute() {
        log.info("[REWARD_NOTIFICATION_EXPORT_CSV] Starting reward notifications export to CSV");
        long startTime = System.currentTimeMillis();

        return initiative2ExportRetrieverService.retrieve()

                .doFinally(x->log.info("[PERFORMANCE_LOG][REWARD_NOTIFICATION_EXPORT_CSV] Reward notification export completed in {}ms", System.currentTimeMillis() - startTime));
    }
}
