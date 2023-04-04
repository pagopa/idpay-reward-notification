package it.gov.pagopa.reward.notification.service.suspension;

import it.gov.pagopa.reward.notification.connector.wallet.WalletRestClient;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardSuspendedUser;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardNotificationRuleRepository;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.repository.RewardsSuspendedUserRepository;
import it.gov.pagopa.reward.notification.utils.AuditUtilities;
import it.gov.pagopa.reward.notification.utils.PerformanceLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Service
@Slf4j
public class UserSuspensionServiceImpl implements UserSuspensionService {

    private final RewardsSuspendedUserRepository rewardsSuspendedUserRepository;
    private final RewardNotificationRuleRepository notificationRuleRepository;
    private final RewardsNotificationRepository rewardsNotificationRepository;
    private final WalletRestClient walletRestClient;

    private final AuditUtilities auditUtilities;

    public UserSuspensionServiceImpl(RewardsSuspendedUserRepository rewardsSuspendedUserRepository, RewardNotificationRuleRepository notificationRuleRepository, RewardsNotificationRepository rewardsNotificationRepository, WalletRestClient walletRestClient, AuditUtilities auditUtilities) {
        this.rewardsSuspendedUserRepository = rewardsSuspendedUserRepository;
        this.notificationRuleRepository = notificationRuleRepository;
        this.rewardsNotificationRepository = rewardsNotificationRepository;
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
    public Mono<Void> readmit(String organizationId, String initiativeId, String userId) {
        log.info("[REWARD_NOTIFICATION][USER_READMISSION] Readmitting suspended user having id {} on initiative {}",
                userId, initiativeId);

        return PerformanceLogger.logTimingFinally("READMISSION",
                notificationRuleRepository.findByInitiativeIdAndOrganizationId(initiativeId, organizationId)
                        .flatMap(i -> rewardsSuspendedUserRepository.findByUserIdAndOrganizationIdAndInitiativeId(
                                                userId,
                                                organizationId,
                                                initiativeId
                                        )
                                        .flatMap(u ->
                                                walletRestClient.readmit(u.getInitiativeId(), u.getUserId())
                                                        .doOnNext(r -> auditUtilities.logReadmission(initiativeId, organizationId, userId))
                                                        .map(r -> u)
                                        )
                                        .flatMap(u ->
                                                rewardsNotificationRepository.findByUserIdAndInitiativeIdAndStatus(u.getUserId(), u.getInitiativeId(), RewardNotificationStatus.SUSPENDED)
                                                        .flatMap(this::readmitNotifications)
                                                        .then(Mono.just(u))
                                        )
                                        .flatMap(rewardsSuspendedUserRepository::delete)
                                        .onErrorResume(e -> {
                                                    auditUtilities.logReadmissionKO(initiativeId, organizationId, userId);
                                                    return Mono.error(e);
                                                }
                                        )
                                // TODO log already active user
                        )

                , "Readmitted user %s".formatted(userId));
    }

    private Mono<RewardsNotification> readmitNotifications(RewardsNotification r) {
        r.setStatus(RewardNotificationStatus.TO_SEND);
        r.setNotificationDate(LocalDate.now());
        return rewardsNotificationRepository.save(r);
    }
}
