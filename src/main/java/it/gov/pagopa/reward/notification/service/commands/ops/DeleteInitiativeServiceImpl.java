package it.gov.pagopa.reward.notification.service.commands.ops;

import it.gov.pagopa.reward.notification.dto.mapper.RewardOrganizationExports2ExportsDTOMapper;
import it.gov.pagopa.reward.notification.model.*;
import it.gov.pagopa.reward.notification.repository.*;
import it.gov.pagopa.reward.notification.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@Slf4j
public class DeleteInitiativeServiceImpl implements DeleteInitiativeService{
    private final RewardNotificationRuleRepository rewardNotificationRuleRepository;
    private final RewardOrganizationExportsRepository rewardOrganizationExportsRepository;
    private final RewardOrganizationImportsRepository rewardOrganizationImportsRepository;
    private final RewardsNotificationRepository rewardsNotificationRepository;
    private final RewardIbanRepository rewardIbanRepository;
    private final RewardsRepository rewardsRepository;
    private final RewardsSuspendedUserRepository rewardsSuspendedUserRepository;
    private final AuditUtilities auditUtilities;

    private final int pageSize;

    private final long delay;

    public DeleteInitiativeServiceImpl(RewardNotificationRuleRepository rewardNotificationRuleRepository,
                                       RewardOrganizationExportsRepository rewardOrganizationExportsRepository,
                                       RewardOrganizationImportsRepository rewardOrganizationImportsRepository,
                                       RewardsNotificationRepository rewardsNotificationRepository,
                                       RewardIbanRepository rewardIbanRepository,
                                       RewardsRepository rewardsRepository,
                                       RewardsSuspendedUserRepository rewardsSuspendedUserRepository,
                                       AuditUtilities auditUtilities,
                                       @Value("${app.delete.paginationSize}") int pageSize,
                                       @Value("${app.delete.delayTime}") long delay) {

        this.rewardNotificationRuleRepository = rewardNotificationRuleRepository;
        this.rewardOrganizationExportsRepository = rewardOrganizationExportsRepository;
        this.rewardOrganizationImportsRepository = rewardOrganizationImportsRepository;
        this.rewardsNotificationRepository = rewardsNotificationRepository;
        this.rewardIbanRepository = rewardIbanRepository;
        this.rewardsRepository = rewardsRepository;
        this.rewardsSuspendedUserRepository = rewardsSuspendedUserRepository;
        this.auditUtilities = auditUtilities;
        this.pageSize = pageSize;
        this.delay = delay;
    }

    @Override
    public Mono<String> execute(String initiativeId) {
        log.info("[DELETE_INITIATIVE] Starting handle delete initiative {}", initiativeId);
        return deleteRewardRuleNotification(initiativeId)
                .then(deleteRewardOrganizationExport(initiativeId))
                .then(deleteRewardOrganizationImport(initiativeId))
                .then(deleteRewardNotification(initiativeId))
                .then(deletedIban(initiativeId))
                .then(deletedRewards(initiativeId))
                .then(deleteRewardSuspendedUser(initiativeId))
                .then(Mono.just(initiativeId));

    }

    private Mono<Void> deleteRewardRuleNotification(String initiativeId){
        return rewardNotificationRuleRepository.deleteById(initiativeId)
                .doOnSuccess(v -> {
                    log.info("[DELETE_INITIATIVE] Deleted initiative {} from collection: reward_notification_rule", initiativeId);
                    auditUtilities.logDeletedRewardRuleNotification(
                        initiativeId);
                });
    }

    private Mono<Void> deleteRewardOrganizationExport(String initiativeId){
        return rewardOrganizationExportsRepository.deleteByInitiativeId(initiativeId)
                .doOnNext(rewardOrganizationExport ->
                    auditUtilities.logDeletedRewardOrgExports(
                            initiativeId,
                            rewardOrganizationExport.getOrganizationId(),
                            retrieveFileName(rewardOrganizationExport.getFilePath())))
                .then()
                .doOnSuccess(i -> log.info("[DELETE_INITIATIVE] Deleted initiative {} from collection: rewards_organization_exports", initiativeId));
    }

    private Mono<Void> deleteRewardOrganizationImport(String initiativeId) {
        return rewardOrganizationImportsRepository.findByInitiativeIdWithBatch(initiativeId, pageSize)
                .flatMap(ri -> rewardOrganizationImportsRepository.deleteById(ri.getFilePath())
                        .then(Mono.just(ri).delayElement(Duration.ofMillis(delay))), pageSize)
                .doOnNext(rewardOrganizationImport ->
                        auditUtilities.logDeletedRewardOrgImports(
                            initiativeId,
                            rewardOrganizationImport.getOrganizationId(),
                            retrieveFileName(rewardOrganizationImport.getFilePath())))
                .then()
                .doOnSuccess(i -> log.info("[DELETE_INITIATIVE] Deleted initiative {} from collection: rewards_organization_imports", initiativeId));

    }

    private Mono<Void> deleteRewardNotification(String initiativeId) {
        return rewardsNotificationRepository.findByInitiativeIdWithBatch(initiativeId, pageSize)
                .flatMap(rn -> rewardsNotificationRepository.deleteById(rn.getId())
                        .then(Mono.just(rn).delayElement(Duration.ofMillis(delay))), pageSize)
                .map(RewardsNotification::getBeneficiaryId)
                .distinct()
                .doOnNext(beneficiaryId -> auditUtilities.logDeletedRewardNotification(initiativeId, beneficiaryId))
                .then()
                .doOnSuccess(i -> log.info("[DELETE_INITIATIVE] Deleted initiative {} from collection: rewards_notification", initiativeId));
    }

    private Mono<Void> deletedIban(String initiativeId){
        return rewardIbanRepository.findByInitiativeIdWithBatch(initiativeId, pageSize)
                .flatMap(ri -> rewardIbanRepository.deleteById(ri.getId())
                        .then(Mono.just(ri).delayElement(Duration.ofMillis(delay))), pageSize)
                .map(RewardIban::getUserId)
                .distinct()
                .doOnNext(userId -> auditUtilities.logDeletedRewardIban(initiativeId, userId))
                .then()
                .doOnNext(i -> log.info("[DELETE_INITIATIVE] Deleted initiative {} from collection: rewards_iban", initiativeId));
    }

    private Mono<Void> deletedRewards(String initiativeId) {
        return rewardsRepository.findByInitiativeIdWithBatch(initiativeId, pageSize)
                .flatMap(r -> rewardsRepository.deleteById(r.getId())
                        .then(Mono.just(r).delayElement(Duration.ofMillis(delay))), pageSize)
                .map(Rewards::getUserId)
                .distinct()
                .doOnNext(userId -> auditUtilities.logDeletedRewards(initiativeId, userId))
                .then()
                .doOnSuccess(i -> log.info("[DELETE_INITIATIVE] Deleted initiative {} from collection: rewards", initiativeId));
    }

    private Mono<Void> deleteRewardSuspendedUser(String initiativeId){
        return rewardsSuspendedUserRepository.findByInitiativeIdWithBatch(initiativeId, pageSize)
                .flatMap(rsu -> rewardsSuspendedUserRepository.deleteById(rsu.getId())
                        .then(Mono.just(rsu).delayElement(Duration.ofMillis(delay))), pageSize)
                .map(RewardSuspendedUser::getUserId)
                .distinct()
                .doOnNext(userId -> auditUtilities.logDeletedSuspendedUser(initiativeId, userId))
                .then()
                .doOnSuccess(i -> log.info("[DELETE_INITIATIVE] Deleted initiative {} from collection: rewards_suspended_users", initiativeId));
    }


    private String retrieveFileName(String filePath) {
        if (filePath == null){
            return "";
        }
        return RewardOrganizationExports2ExportsDTOMapper.retrieveFileName(filePath);
    }
}
