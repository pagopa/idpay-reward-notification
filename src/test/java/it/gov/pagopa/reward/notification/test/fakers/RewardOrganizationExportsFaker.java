package it.gov.pagopa.reward.notification.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import it.gov.pagopa.reward.notification.enums.ExportStatus;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.test.utils.TestUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RewardOrganizationExportsFaker {
    public static final LocalDate CHOSEN_DATE = LocalDate.of(2001,2,4);
    /** It will return an example of {@link RewardOrganizationExport}. Providing a bias, it will return a pseudo-casual object */
    public static RewardOrganizationExport mockInstance(Integer bias){
        return mockInstanceBuilder(bias).build();
    }

    /** It will return an example of builder to obtain a {@link RewardOrganizationExport}. Providing a bias, it will return a pseudo-casual object */
    public static RewardOrganizationExport.RewardOrganizationExportBuilder mockInstanceBuilder(Integer bias){
        RewardOrganizationExport.RewardOrganizationExportBuilder out = RewardOrganizationExport.builder();

        bias = ObjectUtils.firstNonNull(bias, InitiativeRefundDTOFaker.getRandomPositiveNumber(null));

        FakeValuesService fakeValuesService = InitiativeRefundDTOFaker.getFakeValuesService(bias);

        out.id("ID_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.initiativeId("INITIATIVE_ID_%d".formatted(bias));
        out.initiativeName("INITIATIVE_NAME_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.organizationId("ORGANIZATION_ID_%d".formatted(bias));
        out.filePath("/%s/%d".formatted(fakeValuesService.bothify("???"), bias));
        out.notificationDate(CHOSEN_DATE);
        out.exportDate(CHOSEN_DATE);
        out.rewardsExportedCents(bias*10000L);
        out.rewardsResultsCents(bias*10000L);
        out.rewardNotified(bias+1L);
        out.rewardsResulted(bias+1L);
        out.rewardsResultedOk(bias.longValue());
        out.percentageResulted((bias+1L)*1000L);
        out.percentageResultedOk(bias*1000L);
        out.percentageResults((bias*10000L)/50L);
        out.feedbackDate(LocalDateTime.now());
        out.status(ExportStatus.EXPORTED);


        TestUtils.checkNotNullFields(out);
        return out;
    }
}
