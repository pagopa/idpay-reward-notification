package it.gov.pagopa.reward.notification.dto.mapper;

import it.gov.pagopa.reward.notification.dto.controller.RewardImportsDTO;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.function.Function;

@Service
public class RewardOrganizationImport2ImportsDTOMapper  implements Function<RewardOrganizationImport, RewardImportsDTO> {

    private static final DecimalFormat percentageFormatter = new DecimalFormat("0");
    static{
        percentageFormatter.setRoundingMode(RoundingMode.DOWN);
    }


    @Override
    public RewardImportsDTO apply(RewardOrganizationImport rewardOrganizationImport) {
        String filePath = rewardOrganizationImport.getFilePath();
        if(filePath!=null){
            String[] pathSplit = filePath.split("/");
            filePath=pathSplit[pathSplit.length-1];
        }
        return RewardImportsDTO.builder()
                .filePath(filePath)
                .initiativeId(rewardOrganizationImport.getInitiativeId())
                .organizationId(rewardOrganizationImport.getOrganizationId())

                .feedbackDate(rewardOrganizationImport.getFeedbackDate())
                .eTag(rewardOrganizationImport.getETag())
                .contentLength(rewardOrganizationImport.getContentLength())

                .rewardsResulted(rewardOrganizationImport.getRewardsResulted())
                .rewardsResultedError(rewardOrganizationImport.getRewardsResultedError())
                .rewardsResultedOk(rewardOrganizationImport.getRewardsResultedOk())
                .rewardsResultedOkError(rewardOrganizationImport.getRewardsResultedOkError())

                .percentageResulted(percentageFormat(rewardOrganizationImport.getPercentageResulted()))
                .percentageResultedOk(percentageFormat(rewardOrganizationImport.getPercentageResultedOk()))
                .percentageResultedOkElab(percentageFormat(rewardOrganizationImport.getPercentageResultedOkElab()))

                .elabDate(rewardOrganizationImport.getElabDate())
                .exportIds(rewardOrganizationImport.getExportIds())
                .status(rewardOrganizationImport.getStatus())
                .errorsSize(rewardOrganizationImport.getErrorsSize())
                .build();
    }
    private String percentageFormat(Long p) {
        return percentageFormatter.format(p/100);
    }
}
