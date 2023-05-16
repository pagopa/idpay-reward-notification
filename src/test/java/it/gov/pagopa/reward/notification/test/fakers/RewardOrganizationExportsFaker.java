package it.gov.pagopa.reward.notification.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationExportStatus;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.test.utils.TestUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RewardOrganizationExportsFaker {
    public static final LocalDate CHOSEN_DATE = LocalDate.of(2023,2,4);
    /** It will return an example of {@link RewardOrganizationExport}. Providing a bias, it will return a pseudo-casual object */
    public static RewardOrganizationExport mockInstance(Integer bias){
        return mockInstanceBuilder(bias).build();
    }

    /** It will return an example of builder to obtain a {@link RewardOrganizationExport}. Providing a bias, it will return a pseudo-casual object */
    public static RewardOrganizationExport.RewardOrganizationExportBuilder mockInstanceBuilder(Integer bias){
        RewardOrganizationExport.RewardOrganizationExportBuilder out = RewardOrganizationExport.builder();

        bias = ObjectUtils.firstNonNull(bias, InitiativeRefundDTOFaker.getRandomPositiveNumber(null));

        FakeValuesService fakeValuesService = InitiativeRefundDTOFaker.getFakeValuesService(bias);

        String organizationId = "ORGANIZATION_ID_%d".formatted(bias);
        String initiativeId = "INITIATIVE_ID_%d".formatted(bias);

        out.id("ID_%d_%s".formatted(bias, fakeValuesService.bothify("???")))
                .initiativeId(initiativeId)
                .initiativeName("INITIATIVE_NAME_%d_%s".formatted(bias, fakeValuesService.bothify("???")))
                .organizationId(organizationId)
                .filePath("%s/%s/export/dispositive-rewards-%d.zip".formatted(organizationId, initiativeId, bias))
                .notificationDate(CHOSEN_DATE)
                .progressive(0L)
                .exportDate(CHOSEN_DATE)
                .rewardsExportedCents(bias * 10000L)
                .rewardsResultsCents(bias * 10000L)
                .rewardNotified(bias + 1L)
                .rewardsResulted(bias + 1L)
                .rewardsResultedOk(bias.longValue())
                .percentageResulted((bias + 1L) * 1000L)
                .percentageResultedOk(bias * 1000L)
                .percentageResults((bias * 10000L) / 50L)
                .feedbackDate(LocalDateTime.now())
                .status(RewardOrganizationExportStatus.EXPORTED);


        TestUtils.checkNotNullFields(out);
        return out;
    }
}
