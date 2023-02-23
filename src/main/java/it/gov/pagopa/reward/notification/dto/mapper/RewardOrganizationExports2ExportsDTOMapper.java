package it.gov.pagopa.reward.notification.dto.mapper;

import it.gov.pagopa.reward.notification.dto.controller.RewardExportsDTO;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.utils.Utils;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class RewardOrganizationExports2ExportsDTOMapper implements Function<RewardOrganizationExport, RewardExportsDTO> {

    @Override
    public RewardExportsDTO apply(RewardOrganizationExport rewardOrganizationExport) {
        String filePath = rewardOrganizationExport.getFilePath();
        if(filePath!=null){
            String[] pathSplit = filePath.split("/");
            filePath=pathSplit[pathSplit.length-1];
        }
        return RewardExportsDTO.builder()
                .id(rewardOrganizationExport.getId())
                .initiativeId(rewardOrganizationExport.getInitiativeId())
                .initiativeName(rewardOrganizationExport.getInitiativeName())
                .organizationId(rewardOrganizationExport.getOrganizationId())
                .filePath(filePath)
                .notificationDate(rewardOrganizationExport.getNotificationDate())

                .rewardsExported(Utils.cents2Eur(rewardOrganizationExport.getRewardsExportedCents()))
                .rewardsResults(Utils.cents2Eur(rewardOrganizationExport.getRewardsResultsCents()))

                .rewardNotified(rewardOrganizationExport.getRewardNotified())
                .rewardsResulted(rewardOrganizationExport.getRewardsResulted())
                .rewardsResultedOk(rewardOrganizationExport.getRewardsResultedOk())

                .percentageResulted(Utils.percentageFormat(rewardOrganizationExport.getPercentageResulted()))
                .percentageResultedOk(Utils.percentageFormat(rewardOrganizationExport.getPercentageResultedOk()))
                .percentageResults(Utils.percentageFormat(rewardOrganizationExport.getPercentageResults()))

                .feedbackDate(rewardOrganizationExport.getFeedbackDate())
                .status(rewardOrganizationExport.getStatus())
                .build();
    }
}
