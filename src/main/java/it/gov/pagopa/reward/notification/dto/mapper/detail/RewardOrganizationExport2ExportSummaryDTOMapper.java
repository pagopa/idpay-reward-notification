package it.gov.pagopa.reward.notification.dto.mapper.detail;

import it.gov.pagopa.reward.notification.dto.controller.detail.ExportSummaryDTO;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.utils.Utils;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.function.Function;

@Service
public class RewardOrganizationExport2ExportSummaryDTOMapper implements Function<RewardOrganizationExport, ExportSummaryDTO> {

    private static final DecimalFormat percentageFormatter = new DecimalFormat("0");
    static{
        percentageFormatter.setRoundingMode(RoundingMode.DOWN);
    }

    @Override
    public ExportSummaryDTO apply(RewardOrganizationExport rewardOrganizationExport) {
        return ExportSummaryDTO.builder()
                .createDate(rewardOrganizationExport.getExportDate())
                .totalAmount(Utils.cents2Eur(rewardOrganizationExport.getRewardsExportedCents()))
                .totalRefundedAmount(Utils.cents2Eur(rewardOrganizationExport.getRewardsResultsCents()))
                .totalRefunds(rewardOrganizationExport.getRewardNotified())
                .successPercentage(percentageFormat(rewardOrganizationExport.getPercentageResultedOk()))
                .build();
    }

    private String percentageFormat(Long p) {
        return percentageFormatter.format(p/100);
    }
}
