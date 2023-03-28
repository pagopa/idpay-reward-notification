package it.gov.pagopa.reward.notification.service.suspension;

import it.gov.pagopa.reward.notification.connector.wallet.WalletRestClient;
import it.gov.pagopa.reward.notification.model.SuspendedUser;
import it.gov.pagopa.reward.notification.repository.RewardNotificationRuleRepository;
import it.gov.pagopa.reward.notification.repository.SuspendedUsersRepository;
import it.gov.pagopa.reward.notification.utils.AuditUtilities;
import it.gov.pagopa.reward.notification.utils.PerformanceLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class UserSuspensionServiceImpl implements UserSuspensionService {

    private final SuspendedUsersRepository suspendedUsersRepository;
    private final RewardNotificationRuleRepository notificationRuleRepository;
    private final WalletRestClient walletRestClient;

    private final AuditUtilities auditUtilities;

    public UserSuspensionServiceImpl(SuspendedUsersRepository suspendedUsersRepository, RewardNotificationRuleRepository notificationRuleRepository, WalletRestClient walletRestClient, AuditUtilities auditUtilities) {
        this.suspendedUsersRepository = suspendedUsersRepository;
        this.notificationRuleRepository = notificationRuleRepository;
        this.walletRestClient = walletRestClient;
        this.auditUtilities = auditUtilities;
    }

    @Override
    public Mono<SuspendedUser> suspend(String organizationId, String initiativeId, String userId) {
        log.info("[REWARD_NOTIFICATION][USER_SUSPENSION] Suspending user having id {} from initiative {}",
                userId, initiativeId);

        return PerformanceLogger.logTimingFinally("SUSPENSION",
                notificationRuleRepository.findByInitiativeIdAndOrganizationId(initiativeId, organizationId)
                        .flatMap(i -> suspendedUsersRepository.findByUserIdAndOrganizationIdAndInitiativeId(
                                                userId,
                                                organizationId,
                                                initiativeId
                                        )
                                        .doOnNext(u ->
                                                log.info("[REWARD_NOTIFICATION][USER_SUSPENSION] User having id {} already suspended on initiative {}",
                                                        u.getUserId(), u.getInitiativeId())
                                        )
                                        .switchIfEmpty(
                                                suspendedUsersRepository.save(new SuspendedUser(userId, initiativeId, organizationId))
                                                        .flatMap(u ->
                                                                walletRestClient.suspend(u.getInitiativeId(), u.getUserId())
                                                                        .doOnNext(r -> auditUtilities.logSuspension(initiativeId, organizationId, userId))
                                                                        .map(r -> u)
                                                        )
                                                        .onErrorResume(e -> {
                                                            auditUtilities.logSuspensionKO(initiativeId, organizationId, userId);

                                                            return suspendedUsersRepository.deleteById(SuspendedUser.buildId(userId, initiativeId))
                                                                    .then(Mono.error(e));
                                                        })
                                        )
                        )

                , "Suspended user %s".formatted(userId));
    }

    @Override
    public Mono<Boolean> isNotSuspendedUser(String initiativeId, String userId) {
        return suspendedUsersRepository.existsById(SuspendedUser.buildId(userId, initiativeId)).map(b -> !b);
    }
}
