package it.gov.pagopa.reward.notification.service.suspension;

import it.gov.pagopa.reward.notification.connector.wallet.WalletRestClient;
import it.gov.pagopa.reward.notification.model.RewardSuspendedUser;
import it.gov.pagopa.reward.notification.repository.RewardNotificationRuleRepository;
import it.gov.pagopa.reward.notification.repository.RewardsSuspendedUserRepository;
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
    private final WalletRestClient walletRestClient;

    private final AuditUtilities auditUtilities;

    public UserSuspensionServiceImpl(RewardsSuspendedUserRepository rewardsSuspendedUserRepository, RewardNotificationRuleRepository notificationRuleRepository, WalletRestClient walletRestClient, AuditUtilities auditUtilities) {
        this.rewardsSuspendedUserRepository = rewardsSuspendedUserRepository;
        this.notificationRuleRepository = notificationRuleRepository;
        this.walletRestClient = walletRestClient;
        this.auditUtilities = auditUtilities;
    }

    @Override
    public Mono<RewardSuspendedUser> suspend(String organizationId, String initiativeId, String userId) {
        log.info("[REWARD_NOTIFICATION][USER_SUSPENSION] Suspending user having id {} from initiative {}",
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
}
