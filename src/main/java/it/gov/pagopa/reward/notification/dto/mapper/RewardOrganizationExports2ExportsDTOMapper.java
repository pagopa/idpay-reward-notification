package it.gov.pagopa.reward.notification.dto.mapper;

import it.gov.pagopa.common.utils.CommonUtilities;
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
            filePath=retrieveFileName(filePath);
        }
        return RewardExportsDTO.builder()
                .id(rewardOrganizationExport.getId())
                .initiativeId(rewardOrganizationExport.getInitiativeId())
                .initiativeName(rewardOrganizationExport.getInitiativeName())
                .organizationId(rewardOrganizationExport.getOrganizationId())
                .filePath(filePath)
                .notificationDate(rewardOrganizationExport.getNotificationDate())

                .rewardsExported(CommonUtilities.centsToEuroString(rewardOrganizationExport.getRewardsExportedCents()))
                .rewardsResults(CommonUtilities.centsToEuroString(rewardOrganizationExport.getRewardsResultsCents()))

                .rewardNotified(rewardOrganizationExport.getRewardNotified())
                .rewardsResulted(rewardOrganizationExport.getRewardsResulted())
                .rewardsResultedOk(rewardOrganizationExport.getRewardsResultedOk())

                .percentageResulted(Utils.formatAsPercentage(rewardOrganizationExport.getPercentageResulted()))
                .percentageResultedOk(Utils.formatAsPercentage(rewardOrganizationExport.getPercentageResultedOk()))
                .percentageResults(Utils.formatAsPercentage(rewardOrganizationExport.getPercentageResults()))

                .feedbackDate(rewardOrganizationExport.getFeedbackDate())
                .status(rewardOrganizationExport.getStatus())
                .build();
    }
    public static String retrieveFileName(String filePath){
        String[] pathSplit = filePath.split("/");
        return pathSplit[pathSplit.length-1];
    }
}
