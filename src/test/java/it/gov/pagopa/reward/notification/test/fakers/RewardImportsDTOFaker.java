package it.gov.pagopa.reward.notification.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import it.gov.pagopa.reward.notification.dto.controller.RewardImportsDTO;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationImportStatus;
import it.gov.pagopa.reward.notification.test.utils.TestUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class RewardImportsDTOFaker {
    public static final LocalDateTime CHOSEN_DATE = LocalDateTime.now();

    /**
     * It will return an example of {@link RewardImportsDTO}. Providing a bias, it will return a pseudo-casual object
     */
    public static RewardImportsDTO mockInstance(Integer bias) {
        return mockInstanceBuilder(bias).build();
    }

    /**
     * It will return an example of builder to obtain a {@link RewardImportsDTO}. Providing a bias, it will return a pseudo-casual object
     */
    public static RewardImportsDTO.RewardImportsDTOBuilder mockInstanceBuilder(Integer bias) {
        RewardImportsDTO.RewardImportsDTOBuilder out = RewardImportsDTO.builder();

        bias = ObjectUtils.firstNonNull(bias, InitiativeRefundDTOFaker.getRandomPositiveNumber(null));

        FakeValuesService fakeValuesService = InitiativeRefundDTOFaker.getFakeValuesService(bias);

        out.filePath("path%d".formatted(bias));
        out.initiativeId("INITIATIVE_ID_%d".formatted(bias));
        out.organizationId("ORGANIZATION_ID_%d".formatted(bias));
        out.feedbackDate(CHOSEN_DATE);
        out.eTag("ETAG");
        out.contentLength(bias);

        out.rewardsResulted(bias + 1L);
        out.rewardsResultedError(0L);
        out.rewardsResultedOk(bias.longValue());
        out.rewardsResultedOkError(0L);

        out.percentageResulted("%d".formatted((bias + 1L) * 1000L));
        out.percentageResultedOk("%d".formatted(bias * 1000L));
        out.percentageResultedOkElab("%d".formatted(bias * 1000L));

        out.elabDate(CHOSEN_DATE);
        out.exportIds(new ArrayList<>());
        out.status(RewardOrganizationImportStatus.COMPLETE);

        out.errorsSize(0);

        TestUtils.checkNotNullFields(out);
        return out;
    }
}
