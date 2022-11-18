package it.gov.pagopa.reward.notification.dto.mapper;

import it.gov.pagopa.reward.notification.dto.controller.RewardExportsDTO;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.function.Function;

@Service
public class RewardOrganizationExports2ExportsDTOMapper implements Function<RewardOrganizationExport, RewardExportsDTO> {

    public static final DecimalFormatSymbols decimalFormatterSymbols = new DecimalFormatSymbols();
    static{
        decimalFormatterSymbols.setDecimalSeparator(',');
    }
    private static final DecimalFormat decimalFormatter = new DecimalFormat("0.00", decimalFormatterSymbols);
    private static final DecimalFormat percentageFormatter = new DecimalFormat("0");
    static{
        percentageFormatter.setRoundingMode(RoundingMode.DOWN);
    }


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

                .rewardsExported(centsToEur(rewardOrganizationExport.getRewardsExportedCents()))
                .rewardsResults(centsToEur(rewardOrganizationExport.getRewardsResultsCents()))

                .rewardNotified(rewardOrganizationExport.getRewardNotified())
                .rewardsResulted(rewardOrganizationExport.getRewardsResulted())
                .rewardsResultedOk(rewardOrganizationExport.getRewardsResultedOk())

                .percentageResulted(percentageFormat(rewardOrganizationExport.getPercentageResulted()))
                .percentageResultedOk(percentageFormat(rewardOrganizationExport.getPercentageResultedOk()))
                .percentageResults(percentageFormat(rewardOrganizationExport.getPercentageResults()))

                .feedbackDate(rewardOrganizationExport.getFeedbackDate())
                .status(rewardOrganizationExport.getStatus())
                .build();
    }

    private String centsToEur(Long cents) {
        return decimalFormatter.format((double)cents/100);
    }

    private String percentageFormat(Long p) {
        return percentageFormatter.format(p/100);
    }
}
