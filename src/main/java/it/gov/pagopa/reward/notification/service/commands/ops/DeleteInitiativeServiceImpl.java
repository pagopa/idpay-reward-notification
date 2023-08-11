package it.gov.pagopa.reward.notification.service.commands.ops;

import it.gov.pagopa.reward.notification.dto.mapper.RewardOrganizationExports2ExportsDTOMapper;
import it.gov.pagopa.reward.notification.model.*;
import it.gov.pagopa.reward.notification.repository.*;
import it.gov.pagopa.reward.notification.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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

    @SuppressWarnings("squid:S00107") // suppressing too many parameters constructor alert
    public DeleteInitiativeServiceImpl(RewardNotificationRuleRepository rewardNotificationRuleRepository,
                                       RewardOrganizationExportsRepository rewardOrganizationExportsRepository,
                                       RewardOrganizationImportsRepository rewardOrganizationImportsRepository,
                                       RewardsNotificationRepository rewardsNotificationRepository,
                                       RewardIbanRepository rewardIbanRepository,
                                       RewardsRepository rewardsRepository,
                                       RewardsSuspendedUserRepository rewardsSuspendedUserRepository, AuditUtilities auditUtilities) {

        this.rewardNotificationRuleRepository = rewardNotificationRuleRepository;
        this.rewardOrganizationExportsRepository = rewardOrganizationExportsRepository;
        this.rewardOrganizationImportsRepository = rewardOrganizationImportsRepository;
        this.rewardsNotificationRepository = rewardsNotificationRepository;
        this.rewardIbanRepository = rewardIbanRepository;
        this.rewardsRepository = rewardsRepository;
        this.rewardsSuspendedUserRepository = rewardsSuspendedUserRepository;
        this.auditUtilities = auditUtilities;
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
                    log.info("[DELETE_INITIATIVE] Deleted rule notification on initiative {}", initiativeId);
                    auditUtilities.logDeletedRewardRuleNotification(
                        initiativeId);
                });
    }

    private Mono<Void> deleteRewardOrganizationExport(String initiativeId){
        return rewardOrganizationExportsRepository.deleteByInitiativeId(initiativeId)
                .doOnNext(rewardOrganizationExport -> {
                    log.info("[DELETE_INITIATIVE] Deleted organizations exports on initiative {}", initiativeId);
                    auditUtilities.logDeletedRewardOrgExports(
                            initiativeId,
                            rewardOrganizationExport.getOrganizationId(),
                            retrieveFileName(rewardOrganizationExport.getFilePath()));
                })
                .then();
    }

    private Mono<Void> deleteRewardOrganizationImport(String initiativeId) {
        return rewardOrganizationImportsRepository.deleteByInitiativeId(initiativeId)
                .doOnNext(rewardOrganizationImport -> {
                    log.info("[DELETE_INITIATIVE] Deleted organizations imports on initiative {}", initiativeId);
                    auditUtilities.logDeletedRewardOrgImports(
                            initiativeId,
                            rewardOrganizationImport.getOrganizationId(),
                            retrieveFileName(rewardOrganizationImport.getFilePath())
                    );
                })
                .then();
    }

    private Mono<Void> deleteRewardNotification(String initiativeId) {
        return rewardsNotificationRepository.deleteByInitiativeId(initiativeId)
                .map(RewardsNotification::getBeneficiaryId)
                .distinct()
                .doOnNext(beneficiaryId -> {
                    log.info("[DELETE_INITIATIVE] Deleted notifications of the beneficiary {} on initiative {}", beneficiaryId, initiativeId);
                    auditUtilities.logDeletedRewardNotification(initiativeId, beneficiaryId);
                })
                .then();
    }

    private Mono<Void> deletedIban(String initiativeId){
        return rewardIbanRepository.deleteByInitiativeId(initiativeId)
                .map(RewardIban::getUserId)
                .distinct()
                .doOnNext(userId -> {
                    log.info("[DELETE_INITIATIVE] Deleted IBAN of the user {} on initiative {}", userId, initiativeId);
                    auditUtilities.logDeletedRewardIban(userId, initiativeId);
                })
                .then();
    }

    private Mono<Void> deletedRewards(String initiativeId){
        return rewardsRepository.deleteByInitiativeId(initiativeId)
                .map(Rewards::getUserId)
                .distinct()
                .doOnNext(userId -> {
                    log.info("[DELETE_INITIATIVE] Deleted rewards of the user {} on initiative {}", userId, initiativeId);
                    auditUtilities.logDeletedRewards(userId, initiativeId);
                })
                .then();
    }

    private Mono<Void> deleteRewardSuspendedUser(String initiativeId){
        return rewardsSuspendedUserRepository.deleteByInitiativeId(initiativeId)
                .map(RewardSuspendedUser::getUserId)
                .distinct()
                .doOnNext(userId -> {
                    log.info("[DELETE_INITIATIVE] Deleted suspended user {} on initiative {}", userId, initiativeId);
                    auditUtilities.logDeletedSuspendedUser(userId, initiativeId);
                })
                .then();
    }


    private String retrieveFileName(String filePath) {
        if (filePath == null){
            return "";
        }
        return RewardOrganizationExports2ExportsDTOMapper.retrieveFileName(filePath);
    }
}
