package it.gov.pagopa.reward.notification.dto.mapper;

import it.gov.pagopa.reward.notification.dto.controller.RewardExportsDTO;
import it.gov.pagopa.reward.notification.dto.rule.InitiativeRefund2StoreDTO;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;

import java.util.function.Function;

public class RewardOrganizationExports2ExportsDTOMapper implements Function<RewardOrganizationExport, RewardExportsDTO> {

    @Override
    public RewardExportsDTO apply(RewardOrganizationExport rewardOrganizationExport) {
        return RewardExportsDTO.builder()
                .id(rewardOrganizationExport.getId())
                .initiativeId(rewardOrganizationExport.getInitiativeId())
                .initiativeName(rewardOrganizationExport.getInitiativeName())
                .organizationId(rewardOrganizationExport.getOrganizationId())
                .filePath(rewardOrganizationExport.getFilePath())
                .notificationDate(rewardOrganizationExport.getNotificationDate())
                .rewardsExported(rewardOrganizationExport.getRewardsExported())
                .rewardsResults(rewardOrganizationExport.getRewardsResults())
                .feedbackDate(rewardOrganizationExport.getFeedbackDate())
                .status(rewardOrganizationExport.getStatus())
                .build();
    }
}
