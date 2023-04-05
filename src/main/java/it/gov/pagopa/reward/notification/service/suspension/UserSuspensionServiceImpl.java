package it.gov.pagopa.reward.notification.service.suspension;

import it.gov.pagopa.reward.notification.connector.wallet.WalletRestClient;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardSuspendedUser;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardNotificationRuleRepository;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.repository.RewardsSuspendedUserRepository;
import it.gov.pagopa.reward.notification.service.iban.outcome.recovery.DiscardedRewardNotificationService;
import it.gov.pagopa.reward.notification.utils.AuditUtilities;
import it.gov.pagopa.reward.notification.utils.PerformanceLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class UserSuspensionServiceImpl implements UserSuspensionService {

    private final RewardsSuspendedUserRepository rewardsSuspendedUserRepository;
    private final RewardNotificationRuleRepository notificationRuleRepository;
    private final RewardsNotificationRepository rewardsNotificationRepository;
    private final DiscardedRewardNotificationService recoverNotificationService;
    private final WalletRestClient walletRestClient;

    private final AuditUtilities auditUtilities;

    public UserSuspensionServiceImpl(RewardsSuspendedUserRepository rewardsSuspendedUserRepository,
                                     RewardNotificationRuleRepository notificationRuleRepository,
                                     RewardsNotificationRepository rewardsNotificationRepository,
                                     DiscardedRewardNotificationService recoverNotificationService,
                                     WalletRestClient walletRestClient, AuditUtilities auditUtilities) {
        this.rewardsSuspendedUserRepository = rewardsSuspendedUserRepository;
        this.notificationRuleRepository = notificationRuleRepository;
        this.rewardsNotificationRepository = rewardsNotificationRepository;
        this.recoverNotificationService = recoverNotificationService;
        this.walletRestClient = walletRestClient;
        this.auditUtilities = auditUtilities;
    }

    @Override
    public Mono<RewardSuspendedUser> suspend(String organizationId, String initiativeId, String userId) {
        log.info("[REWARD_NOTIFICATION][USER_SUSPENSION] Suspending user having id {} on initiative {}",
                userId, initiativeId);

        return PerformanceLogger.logTimingFinally("SUSPENSION",
                notificationRuleRepository.findByInitiativeIdAndOrganizationId(initiativeId, organizationId)
                        .flatMap(i -> rewardsSuspendedUserRepository.findByUserIdAndOrganizationIdAndInitiativeId(
                                                userId,
                                                organizationId,
                                                initiativeId
                                        )
                                        .doOnNext(u ->
                                                log.info("[REWARD_NOTIFICATION][USER_SUSPENSION] User having id {} already suspended on initiative {}",
                                                        u.getUserId(), u.getInitiativeId())
                                        )
                                        .switchIfEmpty(
                                                rewardsSuspendedUserRepository.save(new RewardSuspendedUser(userId, initiativeId, organizationId))
                                                        .flatMap(u ->
                                                                walletRestClient.suspend(u.getInitiativeId(), u.getUserId())
                                                                        .doOnNext(r -> auditUtilities.logSuspension(initiativeId, organizationId, userId))
                                                                        .map(r -> u)
                                                        )
                                                        .onErrorResume(e -> {
                                                            auditUtilities.logSuspensionKO(initiativeId, organizationId, userId);

                                                            return rewardsSuspendedUserRepository.deleteById(RewardSuspendedUser.buildId(userId, initiativeId))
                                                                    .then(Mono.error(e));
                                                        })
                                        )
                        )

                , "Suspended user %s".formatted(userId));
    }

    @Override
    public Mono<Boolean> isNotSuspendedUser(String initiativeId, String userId) {
        return rewardsSuspendedUserRepository.existsById(RewardSuspendedUser.buildId(userId, initiativeId)).map(b -> !b);
    }

    @Override
    public Mono<RewardSuspendedUser> readmit(String organizationId, String initiativeId, String userId) {
        log.info("[REWARD_NOTIFICATION][USER_READMISSION] Readmitting suspended user having id {} on initiative {}",
                userId, initiativeId);

        RewardSuspendedUser rollbackUser = new RewardSuspendedUser(userId, initiativeId, organizationId);

        return PerformanceLogger.logTimingFinally("READMISSION",
                notificationRuleRepository.findByInitiativeIdAndOrganizationId(initiativeId, organizationId)
                        .flatMap(initiative -> rewardsSuspendedUserRepository.findByUserIdAndOrganizationIdAndInitiativeId(
                                                userId,
                                                organizationId,
                                                initiativeId
                                        )
                                        .flatMap(u -> rewardsSuspendedUserRepository.delete(u)
                                                .then(Mono.just(u))
                                        )
                                        .flatMap(u -> rewardsNotificationRepository.findByUserIdAndInitiativeIdAndStatus(u.getUserId(), u.getInitiativeId(), RewardNotificationStatus.SUSPENDED)
                                                .flatMap(n -> readmitNotifications(initiative, n))
                                                .then(Mono.just(u))
                                        )
                                        .flatMap(u -> walletRestClient.readmit(u.getInitiativeId(), u.getUserId())
                                                .doOnNext(r -> auditUtilities.logReadmission(initiativeId, organizationId, userId))
                                                .map(r -> u)
                                        )
                                        .onErrorResume(e -> {
                                                    auditUtilities.logReadmissionKO(initiativeId, organizationId, userId);
                                                    return rewardsSuspendedUserRepository.save(rollbackUser)
                                                            .then(Mono.error(e));
                                                }
                                        )
                                .switchIfEmpty(Mono.defer(() -> {
                                    log.info("[REWARD_NOTIFICATION][USER_READMISSION] User having id {} already active on initiative {}",
                                            userId, initiativeId);
                                    return Mono.just(rollbackUser);
                                }))
                        )

                , "Readmitted user %s".formatted(userId));
    }

    private Mono<RewardsNotification> readmitNotifications(RewardNotificationRule initiative, RewardsNotification notification) {
        return recoverNotificationService.setRemedialNotificationDate(initiative, notification)
                .doOnNext(n -> n.setStatus(RewardNotificationStatus.TO_SEND))
                .flatMap(rewardsNotificationRepository::save);
    }
}
