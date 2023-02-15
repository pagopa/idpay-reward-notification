package it.gov.pagopa.reward.notification.service.iban.outcome.recovery;

import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardIban;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.notify.RewardNotificationBudgetExhaustedHandlerServiceImpl;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.notify.RewardNotificationTemporalHandlerServiceImpl;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.notify.RewardNotificationThresholdHandlerServiceImpl;
import it.gov.pagopa.reward.notification.service.rule.RewardNotificationRuleService;
import it.gov.pagopa.reward.notification.utils.PerformanceLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class CompletedKoDiscardedRewardNotificationServiceImpl extends BaseDiscardedRewardNotificationServiceImpl implements CompletedKoDiscardedRewardNotificationService {

    public static final String RECOVERY_ID_SUFFIX = "_recovery-";
    private final RewardsNotificationRepository rewardsNotificationRepository;


    public CompletedKoDiscardedRewardNotificationServiceImpl(RewardsNotificationRepository rewardsNotificationRepository,
                                                             RewardNotificationRuleService notificationRuleService,
                                                             RewardNotificationTemporalHandlerServiceImpl temporalHandler,
                                                             RewardNotificationBudgetExhaustedHandlerServiceImpl budgetExhaustedHandler,
                                                             RewardNotificationThresholdHandlerServiceImpl thresholdHandler) {
        super(notificationRuleService, temporalHandler, budgetExhaustedHandler, thresholdHandler);
        this.rewardsNotificationRepository = rewardsNotificationRepository;
    }

    @Override
    public Mono<List<RewardsNotification>> handleCompletedKoDiscardedRewardNotification(RewardIban rewardIban) {
        log.info("[REWARD_NOTIFICATION_IBAN_OUTCOME] Searching for {} discarded rewardNotification on userId {} and initiativeId {}",
                RewardNotificationStatus.COMPLETED_KO, rewardIban.getUserId(), rewardIban.getInitiativeId());

        // case 2 - Exported notification having KO result
        return PerformanceLogger.logTimingOnNext("REWARD_NOTIFICATION_IBAN_OUTCOME",
                rewardsNotificationRepository.findByUserIdAndInitiativeIdAndStatusAndRemedialIdNull(
                                rewardIban.getUserId(),
                                rewardIban.getInitiativeId(),
                                RewardNotificationStatus.COMPLETED_KO)
                        .flatMap(this::createRemedialNotification)
                        .collectList(),
                recovered -> "Recovered %d %s rewardNotification on userId %s and initiativeId %s".formatted(
                        recovered.size(),
                        RewardNotificationStatus.COMPLETED_KO,
                        rewardIban.getUserId(),
                        rewardIban.getInitiativeId()));
    }

    private Mono<RewardsNotification> createRemedialNotification(RewardsNotification discarded) {
        log.info("[REWARD_NOTIFICATION_IBAN_OUTCOME] Found discarded {}} rewardNotification having id {} on userId {} and initiativeId {}",
                RewardNotificationStatus.COMPLETED_KO, discarded.getId(), discarded.getUserId(), discarded.getInitiativeId());

        return Mono.just(discarded)
                .flatMap(this::buildRemedialNotification)
                .flatMap(remedialNotification -> updateDiscardedAndStoreRemedial(discarded, remedialNotification))

                .onErrorResume(e -> {
                    log.error("[REWARD_NOTIFICATION_IBAN_OUTCOME] Something went wrong while recovering {} rewardNotification having id {} related to userId {} and initiativeId {}",
                            RewardNotificationStatus.COMPLETED_KO, discarded.getId(), discarded.getUserId(), discarded.getInitiativeId(), e);
                    return Mono.empty();
                });
    }

    private Mono<RewardsNotification> buildRemedialNotification(RewardsNotification discarded) {

        RewardsNotification remedial = discarded.toBuilder()
                .status(RewardNotificationStatus.TO_SEND)
                .exportId(null)
                .exportDate(null)
                .iban(null)
                .rejectionReason(null)
                .resultCode(null)
                .feedbackDate(null)
                .feedbackHistory(Collections.emptyList())
                .cro(null)
                .executionDate(null)
                .remedialId(null)
                .build();

        setNewIdAndOrdinaryId(remedial, discarded);


        return setRemedialNotificationDate(discarded.getInitiativeId(), remedial);
    }

    private void setNewIdAndOrdinaryId(RewardsNotification out, RewardsNotification input) {
        String id = input.getId();

        // if recovering a remedial it will calculate the next progressive
        if (input.getOrdinaryId() != null) {
            int nextRecoveryProgressiveId = getNextRecoveryProgressiveId(id);

            out.setId("%s%s%d".formatted(input.getOrdinaryId(), RECOVERY_ID_SUFFIX, nextRecoveryProgressiveId));
            out.setExternalId("%s%s%d".formatted(input.getExternalId(), RECOVERY_ID_SUFFIX, nextRecoveryProgressiveId));
            out.setOrdinaryId(input.getOrdinaryId());
        } else {
            out.setId(id.concat("%s1".formatted(RECOVERY_ID_SUFFIX)));
            out.setExternalId(input.getExternalId().concat("%s1".formatted(RECOVERY_ID_SUFFIX)));
            out.setOrdinaryId(id);
        }

    }

    private static int getNextRecoveryProgressiveId(String id) {
        String[] idSplit = id.split(RECOVERY_ID_SUFFIX);

        return idSplit.length == 2 ? Integer.parseInt(idSplit[1]) + 1 : 1;
    }

    private Mono<RewardsNotification> updateDiscardedAndStoreRemedial(RewardsNotification discarded, RewardsNotification remedial) {

        discarded.setRemedialId(remedial.getId());
        discarded.setStatus(RewardNotificationStatus.RECOVERED);

        return rewardsNotificationRepository.save(discarded)
                .flatMap(x -> rewardsNotificationRepository.save(remedial));
    }
}
