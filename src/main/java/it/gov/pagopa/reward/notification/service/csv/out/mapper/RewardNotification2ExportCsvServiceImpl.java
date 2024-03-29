package it.gov.pagopa.reward.notification.service.csv.out.mapper;

import it.gov.pagopa.common.reactive.utils.PerformanceLogger;
import it.gov.pagopa.reward.notification.dto.mapper.RewardNotificationExport2CsvMapper;
import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationExportCsvDto;
import it.gov.pagopa.reward.notification.enums.BeneficiaryType;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.csv.out.retrieve.Iban2NotifyRetrieverService;
import it.gov.pagopa.reward.notification.service.csv.out.retrieve.Merchant2NotifyRetrieverService;
import it.gov.pagopa.reward.notification.service.csv.out.retrieve.User2NotifyRetrieverService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class RewardNotification2ExportCsvServiceImpl implements RewardNotification2ExportCsvService {

    private final RewardsNotificationRepository rewardsNotificationRepository;
    private final RewardNotificationExport2CsvMapper rewardNotificationExport2CsvMapper;
    private final Iban2NotifyRetrieverService iban2NotifyRetrieverService;
    private final User2NotifyRetrieverService user2NotifyRetrieverService;
    private final Merchant2NotifyRetrieverService merchant2NotifyRetrieverService;

    public RewardNotification2ExportCsvServiceImpl(
            RewardsNotificationRepository rewardsNotificationRepository,
            Iban2NotifyRetrieverService iban2NotifyRetrieverService,
            User2NotifyRetrieverService user2NotifyRetrieverService,
            RewardNotificationExport2CsvMapper rewardNotificationExport2CsvMapper,
            Merchant2NotifyRetrieverService merchant2NotifyRetrieverService) {
        this.rewardsNotificationRepository = rewardsNotificationRepository;
        this.iban2NotifyRetrieverService = iban2NotifyRetrieverService;
        this.user2NotifyRetrieverService = user2NotifyRetrieverService;
        this.rewardNotificationExport2CsvMapper = rewardNotificationExport2CsvMapper;
        this.merchant2NotifyRetrieverService = merchant2NotifyRetrieverService;
    }

    @Override
    public Mono<RewardNotificationExportCsvDto> apply(RewardsNotification reward) {
        long startTime = System.currentTimeMillis();
        return PerformanceLogger.logTimingOnNext(
                        "EXPORT_CSV_DATA_ENRICH", startTime,
                        BeneficiaryType.MERCHANT.equals(reward.getBeneficiaryType())
                                ? merchantNotification2ExportCsv(reward)
                                : citizenNotification2ExportCsv(reward),
                        x -> reward.getId()
                )
                .onErrorResume(e -> {
                    log.error("[REWARD_NOTIFICATION_EXPORT_CSV] Something gone wrong while trying to map reward {} on csv line", reward.getId(), e);
                    PerformanceLogger.logTiming("EXPORT_CSV_DATA_ENRICH", startTime, "%s - FAIL".formatted(reward.getId()));
                    return Mono.empty();
                });
    }

    private Mono<RewardNotificationExportCsvDto> citizenNotification2ExportCsv(RewardsNotification reward) {
        return Mono.just(reward)
                .flatMap(this::checkRewardAmount)
                .flatMap(iban2NotifyRetrieverService::retrieveIban)
                .flatMap(user2NotifyRetrieverService::retrieveUser)
                .map(r2user -> rewardNotificationExport2CsvMapper.apply(r2user.getKey(), r2user.getValue()));
    }

    private Mono<RewardNotificationExportCsvDto> merchantNotification2ExportCsv(RewardsNotification reward) {
        return Mono.just(reward)
                .flatMap(this::checkRewardAmount)
                .flatMap(merchant2NotifyRetrieverService::retrieve)
                .map(r2merchant -> rewardNotificationExport2CsvMapper.apply(r2merchant.getKey(), r2merchant.getValue()));
    }

    private Mono<RewardsNotification> checkRewardAmount(RewardsNotification notification) {
        if(notification.getRewardCents() == 0L){
            log.info("[REWARD_NOTIFICATION_EXPORT_CSV] Skipping export of notification because has reward 0 {}; beneficiaryId {} beneficiaryType {} initiativeId {}",
                    notification.getId(), notification.getBeneficiaryId(), notification.getBeneficiaryType(), notification.getInitiativeId());
            notification.setStatus(RewardNotificationStatus.SKIPPED);
            return rewardsNotificationRepository.save(notification)
                    .then(Mono.empty());
        } else {
            return Mono.just(notification);
        }
    }
}
