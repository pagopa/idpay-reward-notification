package it.gov.pagopa.reward.notification.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import it.gov.pagopa.reward.notification.dto.controller.RewardExportsDTO;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationExportStatus;
import it.gov.pagopa.reward.notification.test.utils.TestUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RewardExportsDTOFaker {
    public static final LocalDate CHOSEN_DATE = LocalDate.of(2001, 2, 4);

    /**
     * It will return an example of {@link RewardExportsDTO}. Providing a bias, it will return a pseudo-casual object
     */
    public static RewardExportsDTO mockInstance(Integer bias) {
        return mockInstanceBuilder(bias).build();
    }

    /**
     * It will return an example of builder to obtain a {@link RewardExportsDTO}. Providing a bias, it will return a pseudo-casual object
     */
    public static RewardExportsDTO.RewardExportsDTOBuilder mockInstanceBuilder(Integer bias) {
        RewardExportsDTO.RewardExportsDTOBuilder out = RewardExportsDTO.builder();

        bias = ObjectUtils.firstNonNull(bias, InitiativeRefundDTOFaker.getRandomPositiveNumber(null));

        FakeValuesService fakeValuesService = InitiativeRefundDTOFaker.getFakeValuesService(bias);

        out.id("ID_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.initiativeId("INITIATIVE_ID_%d".formatted(bias));
        out.initiativeName("INITIATIVE_NAME_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.organizationId("ORGANIZATION_ID_%d".formatted(bias));
        out.filePath("/%s/%d".formatted(fakeValuesService.bothify("???"), bias));
        out.notificationDate(CHOSEN_DATE);
        out.rewardsExported("%d".formatted(bias * 100L));
        out.rewardsResults("%d".formatted(bias * 100L));
        out.rewardNotified(bias + 1L);
        out.rewardsResulted(bias + 1L);
        out.rewardsResultedOk(bias.longValue());
        out.percentageResulted("%d".formatted((bias + 1L) * 1000L));
        out.percentageResultedOk("%d".formatted(bias * 1000L));
        out.percentageResults("%d".formatted((bias * 10000L) / 50L));
        out.feedbackDate(LocalDateTime.now());
        out.status(RewardOrganizationExportStatus.EXPORTED);


        TestUtils.checkNotNullFields(out);
        return out;
    }
}
