package it.gov.pagopa.reward.notification.service.iban.outcome.recovery;

import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardIban;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.RewardsNotificationDateReschedulerService;
import it.gov.pagopa.common.reactive.utils.PerformanceLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class CompletedKoDiscardedRewardNotificationServiceImpl implements CompletedKoDiscardedRewardNotificationService {

    public static final String RECOVERY_ID_SUFFIX = "_recovery-";
    public static final String RECOVERED_ID_PLACEHOLDERS = "%s%s%d";
    private final RewardsNotificationRepository rewardsNotificationRepository;
    private final RewardsNotificationDateReschedulerService notificationDateReschedulerService;


    public CompletedKoDiscardedRewardNotificationServiceImpl(RewardsNotificationRepository rewardsNotificationRepository,
                                                             RewardsNotificationDateReschedulerService notificationDateReschedulerService) {
        this.rewardsNotificationRepository = rewardsNotificationRepository;
        this.notificationDateReschedulerService = notificationDateReschedulerService;
    }

    @Override
    public Mono<List<RewardsNotification>> handleCompletedKoDiscardedRewardNotification(RewardIban rewardIban) {
        log.info("[REWARD_NOTIFICATION_IBAN_OUTCOME] [IBAN_OUTCOME_RECOVER_COMPLETED_KO] Searching for COMPLETED_KO discarded rewardNotification on userId {} and initiativeId {}",
                rewardIban.getUserId(), rewardIban.getInitiativeId());

        // case 2 - Exported notification having KO result
        return PerformanceLogger.logTimingOnNext("IBAN_OUTCOME_RECOVER_COMPLETED_KO",
                rewardsNotificationRepository.findByBeneficiaryIdAndInitiativeIdAndStatusAndRemedialIdNull(
                                rewardIban.getUserId(),
                                rewardIban.getInitiativeId(),
                                RewardNotificationStatus.COMPLETED_KO)
                        .flatMap(this::createRemedialNotification)
                        .collectList(),
                recovered -> "Recovered %d COMPLETED_KO rewardNotification on userId %s and initiativeId %s".formatted(
                        recovered.size(),
                        rewardIban.getUserId(),
                        rewardIban.getInitiativeId()));
    }

    private Mono<RewardsNotification> createRemedialNotification(RewardsNotification discarded) {
        log.info("[REWARD_NOTIFICATION_IBAN_OUTCOME] [IBAN_OUTCOME_RECOVER_COMPLETED_KO] Found discarded COMPLETED_KO rewardNotification having id {} on beneficiaryId {} beneficiaryType {} initiativeId {}",
                discarded.getId(), discarded.getBeneficiaryId(), discarded.getBeneficiaryType(), discarded.getInitiativeId());

        return Mono.just(discarded)
                .flatMap(this::buildRemedialNotification)
                .flatMap(remedialNotification -> updateDiscardedAndStoreRemedial(discarded, remedialNotification))

                .onErrorResume(e -> {
                    log.error("[REWARD_NOTIFICATION_IBAN_OUTCOME] [IBAN_OUTCOME_RECOVER_COMPLETED_KO] Something went wrong while recovering COMPLETED_KO rewardNotification having id {} related to beneficiaryId {} beneficiaryType {} initiativeId {}",
                            discarded.getId(), discarded.getBeneficiaryId(), discarded.getBeneficiaryType(), discarded.getInitiativeId(), e);
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

        setRemedialNotificationIds(discarded, remedial);


        return notificationDateReschedulerService.setHandledNotificationDate(remedial);
    }

    private void setRemedialNotificationIds(RewardsNotification discarded, RewardsNotification remedial) {
        String id = discarded.getId();
        String externalId = discarded.getExternalId();

        // if recovering a remedial it will calculate the next progressive
        if (discarded.getOrdinaryId() != null) {

            remedial.setId(buildRecoveryId(id, discarded.getOrdinaryId()));
            remedial.setExternalId(buildRecoveryId(externalId, discarded.getOrdinaryExternalId()));

            remedial.setOrdinaryId(discarded.getOrdinaryId());
            remedial.setOrdinaryExternalId(discarded.getOrdinaryExternalId());
        } else {
            remedial.setId(buildFirstRecoveryId(id));
            remedial.setExternalId(buildFirstRecoveryId(externalId));

            remedial.setOrdinaryId(id);
            remedial.setOrdinaryExternalId(externalId);
        }

        remedial.setRecoveredId(discarded.getId());
        remedial.setRecoveredExternalId(discarded.getExternalId());
    }

    /**
     * Returns a formatted String with given base id, {@link CompletedKoDiscardedRewardNotificationServiceImpl#RECOVERY_ID_SUFFIX}
     * and the calculated next recovery progressive
     */
    private String buildRecoveryId(String id, String baseId) {
        int nextRecoveryProgressive = getNextRecoveryProgressive(id);
        return buildRecoveryId(baseId, nextRecoveryProgressive);
    }

    private String buildFirstRecoveryId(String baseId) {
        return buildRecoveryId(baseId, 1);
    }

    private String buildRecoveryId(String baseId, int progressive) {
        return RECOVERED_ID_PLACEHOLDERS.formatted(baseId, RECOVERY_ID_SUFFIX, progressive);
    }

    private int getNextRecoveryProgressive(String id) {
        String[] idSplit = id.split(RECOVERY_ID_SUFFIX);

        return idSplit.length == 2 ? Integer.parseInt(idSplit[1]) + 1 : 1;
    }

    private Mono<RewardsNotification> updateDiscardedAndStoreRemedial(RewardsNotification discarded, RewardsNotification remedial) {
        discarded.setRemedialId(remedial.getId());
        discarded.setStatus(RewardNotificationStatus.RECOVERED);

        return rewardsNotificationRepository.saveIfNotExists(remedial)
                .then(rewardsNotificationRepository.save(discarded))
                .then(Mono.just(remedial));
    }
}
