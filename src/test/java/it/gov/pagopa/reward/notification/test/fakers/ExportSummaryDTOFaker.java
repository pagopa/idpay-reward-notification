package it.gov.pagopa.reward.notification.test.fakers;

import it.gov.pagopa.reward.notification.dto.controller.detail.ExportSummaryDTO;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationExportStatus;
import org.apache.commons.lang3.ObjectUtils;

import java.time.LocalDate;
import java.util.Random;

public class ExportSummaryDTOFaker {

    private static final Random randomGenerator = new Random();

    public static Random getRandom(Integer bias) {
        return bias == null ? randomGenerator : new Random(bias);
    }

    public static int getRandomPositiveNumber(Integer bias) {
        return Math.abs(getRandom(bias).nextInt());
    }

    public static ExportSummaryDTO mockInstance(Integer bias){
        return mockInstanceBuilder(bias).build();
    }

    public static ExportSummaryDTO mockInstance(Integer bias, LocalDate date){
        return mockInstanceBuilder(bias, date).build();
    }

    public static ExportSummaryDTO.ExportSummaryDTOBuilder mockInstanceBuilder(Integer bias){
        ExportSummaryDTO.ExportSummaryDTOBuilder out = ExportSummaryDTO.builder();

        bias = ObjectUtils.firstNonNull(bias, getRandomPositiveNumber(null));

        out.createDate(LocalDate.now());
        out.totalAmountCents(bias*100L);
        out.totalRefundedAmountCents(bias*10L);
        out.totalRefunds(bias+1L);
        out.successPercentage("10");
        out.status(RewardOrganizationExportStatus.EXPORTED);

        return out;
    }

    public static ExportSummaryDTO.ExportSummaryDTOBuilder mockInstanceBuilder(Integer bias, LocalDate date){
        ExportSummaryDTO.ExportSummaryDTOBuilder out = ExportSummaryDTO.builder();

        bias = ObjectUtils.firstNonNull(bias, getRandomPositiveNumber(null));

        out.createDate(date);
        out.totalAmountCents(bias*100L);
        out.totalRefundedAmountCents(bias*10L);
        out.totalRefunds(bias+1L);
        out.successPercentage("10");
        out.status(RewardOrganizationExportStatus.EXPORTED);

        return out;
    }
}
