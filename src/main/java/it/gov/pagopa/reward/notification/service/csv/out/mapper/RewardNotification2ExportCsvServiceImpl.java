package it.gov.pagopa.reward.notification.service.csv.out.mapper;

import it.gov.pagopa.reward.notification.dto.mapper.RewardNotificationExport2CsvMapper;
import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationExportCsvDto;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.service.csv.out.retrieve.Iban2NotifyRetrieverService;
import it.gov.pagopa.reward.notification.service.csv.out.retrieve.User2NotifyRetrieverService;
import it.gov.pagopa.reward.notification.utils.PerformanceLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class RewardNotification2ExportCsvServiceImpl implements RewardNotification2ExportCsvService {

    private final Iban2NotifyRetrieverService iban2NotifyRetrieverService;
    private final User2NotifyRetrieverService user2NotifyRetrieverService;
    private final RewardNotificationExport2CsvMapper rewardNotificationExport2CsvMapper;

    public RewardNotification2ExportCsvServiceImpl(Iban2NotifyRetrieverService iban2NotifyRetrieverService, User2NotifyRetrieverService user2NotifyRetrieverService, RewardNotificationExport2CsvMapper rewardNotificationExport2CsvMapper) {
        this.iban2NotifyRetrieverService = iban2NotifyRetrieverService;
        this.user2NotifyRetrieverService = user2NotifyRetrieverService;
        this.rewardNotificationExport2CsvMapper = rewardNotificationExport2CsvMapper;
    }

    @Override
    public Mono<RewardNotificationExportCsvDto> apply(RewardsNotification reward) {
        long startTime = System.currentTimeMillis();
        return PerformanceLogger.logTimingOnNext(
                        "EXPORT_CSV_DATA_ENRICH", startTime,
                        iban2NotifyRetrieverService.retrieveIban(reward)
                                .flatMap(user2NotifyRetrieverService::retrieveUser)
                                .map(r2user -> rewardNotificationExport2CsvMapper.apply(r2user.getKey(), r2user.getValue())),
                        x -> reward.getId()
                )
                .onErrorResume(e -> {
                    log.error("[REWARD_NOTIFICATION_EXPORT_CSV] Something gone wrong while trying to map reward {} on csv line", reward.getId(), e);
                    PerformanceLogger.logTiming("EXPORT_CSV_DATA_ENRICH", startTime, "%s - FAIL".formatted(reward.getId()));
                    return Mono.empty();
                });
    }
}
