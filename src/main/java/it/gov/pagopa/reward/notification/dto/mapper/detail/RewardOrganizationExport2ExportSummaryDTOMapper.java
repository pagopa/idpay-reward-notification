package it.gov.pagopa.reward.notification.dto.mapper.detail;

import it.gov.pagopa.reward.notification.dto.controller.detail.ExportSummaryDTO;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.utils.Utils;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class RewardOrganizationExport2ExportSummaryDTOMapper implements Function<RewardOrganizationExport, ExportSummaryDTO> {


    @Override
    public ExportSummaryDTO apply(RewardOrganizationExport rewardOrganizationExport) {
        return ExportSummaryDTO.builder()
                .createDate(rewardOrganizationExport.getExportDate())
                .totalAmountCents(rewardOrganizationExport.getRewardsExportedCents())
                .totalRefundedAmountCents(rewardOrganizationExport.getRewardsResultsCents())
                .totalRefunds(rewardOrganizationExport.getRewardNotified())
                .successPercentage(Utils.formatAsPercentage(rewardOrganizationExport.getPercentageResultedOk()))
                .status(rewardOrganizationExport.getStatus())
                .build();
    }
}
