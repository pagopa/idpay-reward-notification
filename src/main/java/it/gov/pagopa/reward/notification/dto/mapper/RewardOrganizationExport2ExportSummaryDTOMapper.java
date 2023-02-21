package it.gov.pagopa.reward.notification.dto.mapper;

import it.gov.pagopa.reward.notification.dto.controller.ExportSummaryDTO;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
                .totalAmount(centsToEur(rewardOrganizationExport.getRewardsExportedCents()))
                .totalRefundedAmount(centsToEur(rewardOrganizationExport.getRewardsResultsCents()))
                .totalRefunds(rewardOrganizationExport.getRewardNotified())
                .successPercentage(percentageFormat(rewardOrganizationExport.getPercentageResultedOk()))
                .build();
    }

    private BigDecimal centsToEur(Long cents) {
        return BigDecimal.valueOf(cents/100);
    }

    private String percentageFormat(Long p) {
        return percentageFormatter.format(p/100);
    }
}
