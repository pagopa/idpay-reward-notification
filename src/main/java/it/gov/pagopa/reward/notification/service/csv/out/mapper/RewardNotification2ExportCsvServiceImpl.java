package it.gov.pagopa.reward.notification.service.csv.out.mapper;

import it.gov.pagopa.reward.notification.dto.mapper.RewardNotificationExport2CsvMapper;
import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationExportCsvDto;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.csv.out.retrieve.Iban2NotifyRetrieverService;
import it.gov.pagopa.reward.notification.service.csv.out.retrieve.User2NotifyRetrieverService;
import it.gov.pagopa.reward.notification.utils.PerformanceLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
public class RewardNotification2ExportCsvServiceImpl implements RewardNotification2ExportCsvService {

    private final RewardsNotificationRepository rewardsNotificationRepository;
    private final RewardNotificationExport2CsvMapper rewardNotificationExport2CsvMapper;
    private final Iban2NotifyRetrieverService iban2NotifyRetrieverService;
    private final User2NotifyRetrieverService user2NotifyRetrieverService;

    public RewardNotification2ExportCsvServiceImpl(
            RewardsNotificationRepository rewardsNotificationRepository,
            Iban2NotifyRetrieverService iban2NotifyRetrieverService,
            User2NotifyRetrieverService user2NotifyRetrieverService,
            RewardNotificationExport2CsvMapper rewardNotificationExport2CsvMapper) {
        this.rewardsNotificationRepository = rewardsNotificationRepository;
        this.iban2NotifyRetrieverService = iban2NotifyRetrieverService;
        this.user2NotifyRetrieverService = user2NotifyRetrieverService;
        this.rewardNotificationExport2CsvMapper = rewardNotificationExport2CsvMapper;
    }

    @Override
    public Mono<RewardNotificationExportCsvDto> apply(RewardsNotification reward) {
        long startTime = System.currentTimeMillis();
        return PerformanceLogger.logTimingOnNext(
                        "EXPORT_CSV_DATA_ENRICH", startTime,
                        Mono.just(reward)
                                .flatMap(this::checkRewardAmount)
                                .flatMap(iban2NotifyRetrieverService::retrieveIban)
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

    private Mono<RewardsNotification> checkRewardAmount(RewardsNotification notification) {
        if(notification.getRewardCents() == 0L){
            log.info("[REWARD_NOTIFICATION_EXPORT_CSV] Skipping export of notification because has reward 0 {}; beneficiaryId {} beneficiaryType {} initiativeId {}",
                    notification.getId(), notification.getBeneficiaryId(), notification.getBeneficiaryType(), notification.getInitiativeId());
            notification.setStatus(RewardNotificationStatus.SKIPPED);
            notification.setExportDate(LocalDateTime.now());
            return rewardsNotificationRepository.save(notification)
                    .then(Mono.empty());
        } else {
            return Mono.just(notification);
        }
    }
}
