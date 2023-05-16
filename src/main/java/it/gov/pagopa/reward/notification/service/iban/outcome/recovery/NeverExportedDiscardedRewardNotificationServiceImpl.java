package it.gov.pagopa.reward.notification.service.iban.outcome.recovery;

import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardIban;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.RewardsNotificationDateReschedulerService;
import it.gov.pagopa.reward.notification.utils.ExportCsvConstants;
import it.gov.pagopa.reward.notification.utils.PerformanceLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
public class NeverExportedDiscardedRewardNotificationServiceImpl implements NeverExportedDiscardedRewardNotificationService{

    private final RewardsNotificationRepository rewardsNotificationRepository;
    private final RewardsNotificationDateReschedulerService notificationDateReschedulerService;

    public NeverExportedDiscardedRewardNotificationServiceImpl(RewardsNotificationRepository rewardsNotificationRepository,
                                                               RewardsNotificationDateReschedulerService notificationDateReschedulerService) {
        this.rewardsNotificationRepository = rewardsNotificationRepository;
        this.notificationDateReschedulerService = notificationDateReschedulerService;
    }

    @Override
    public Mono<List<RewardsNotification>> handleNeverExportedDiscardedRewardNotification(RewardIban rewardIban) {
        log.info("[REWARD_NOTIFICATION_IBAN_OUTCOME] [IBAN_OUTCOME_RECOVER_ERROR_IBAN] Searching for never exported discarded rewardNotification on userId {} and initiativeId {}",
                rewardIban.getUserId(), rewardIban.getInitiativeId());

        // case 1 - Notification never exported
        return PerformanceLogger.logTimingOnNext("IBAN_OUTCOME_RECOVER_ERROR_IBAN",
                rewardsNotificationRepository.findByBeneficiaryIdAndInitiativeIdAndStatusAndRejectionReasonAndExportIdNull(
                        rewardIban.getUserId(),
                        rewardIban.getInitiativeId(),
                        RewardNotificationStatus.ERROR,
                        ExportCsvConstants.EXPORT_REJECTION_REASON_IBAN_NOT_FOUND)
                .flatMap(this::recoverNeverExportedDiscardedRewardNotification)
                .collectList(),
                recovered -> "Recovered %d never exported rewardNotification on userId %s and initiativeId %s".formatted(
                        recovered.size(),
                        rewardIban.getUserId(),
                        rewardIban.getInitiativeId()));
    }

    private Mono<RewardsNotification> recoverNeverExportedDiscardedRewardNotification(RewardsNotification notification) {
        log.info("[REWARD_NOTIFICATION_IBAN_OUTCOME] [IBAN_OUTCOME_RECOVER_ERROR_IBAN] Found discarded never exported rewardNotification having id {} on userId {} and initiativeId {}",
                notification.getId(), notification.getBeneficiaryId(), notification.getInitiativeId());

        return Mono.just(notification)
                .doOnNext(this::resetRewardNotificationStatus)
                .flatMap(notificationDateReschedulerService::setHandledNotificationDate)
                .flatMap(rewardsNotificationRepository::save)
                .onErrorResume(e -> {
                    log.error("[REWARD_NOTIFICATION_IBAN_OUTCOME] [IBAN_OUTCOME_RECOVER_ERROR_IBAN] Something went wrong while recovering never exported rewardNotification having id {} related to userId {} and initiativeId {}",
                            notification.getId(), notification.getBeneficiaryId(), notification.getInitiativeId(), e);
                    return Mono.empty();
                });
    }

    private void resetRewardNotificationStatus(RewardsNotification rewardsNotification) {
        rewardsNotification.setStatus(RewardNotificationStatus.TO_SEND);
        rewardsNotification.setRejectionReason(null);
        rewardsNotification.setResultCode(null);
        rewardsNotification.setExportDate(null);
    }
}
