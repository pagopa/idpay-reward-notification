package it.gov.pagopa.reward.notification.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.notification.dto.rule.AccumulatedAmountDTO;
import it.gov.pagopa.reward.notification.dto.rule.TimeParameterDTO;
import it.gov.pagopa.reward.notification.enums.InitiativeRewardType;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import org.apache.commons.lang3.ObjectUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RewardNotificationRuleFaker {
    /** It will return an example of {@link RewardNotificationRule}. Providing a bias, it will return a pseudo-casual object */
    public static RewardNotificationRule mockInstance(Integer bias){
        return mockInstanceBuilder(bias).build();
    }

    /** It will return an example of builder to obtain a {@link RewardNotificationRule}. Providing a bias, it will return a pseudo-casual object */
    public static RewardNotificationRule.RewardNotificationRuleBuilder mockInstanceBuilder(Integer bias){
        RewardNotificationRule.RewardNotificationRuleBuilder out = RewardNotificationRule.builder();

        bias = ObjectUtils.firstNonNull(bias, InitiativeRefundDTOFaker.getRandomPositiveNumber(null));

        FakeValuesService fakeValuesService = InitiativeRefundDTOFaker.getFakeValuesService(bias);

        out.initiativeId("ID_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.initiativeName("NAME_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.endDate(LocalDate.now());
        out.organizationId("ORGANIZATION_ID_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.updateDate(LocalDateTime.now());

        AccumulatedAmountDTO accumulatedAmount = AccumulatedAmountDTO.builder()
                .accumulatedType(AccumulatedAmountDTO.AccumulatedTypeEnum.THRESHOLD_REACHED)
                .refundThresholdCents(10000L)
                .build();
        out.accumulatedAmount(accumulatedAmount);

        TimeParameterDTO timeParameter = TimeParameterDTO.builder()
                .timeType(TimeParameterDTO.TimeTypeEnum.DAILY)
                .build();
        out.timeParameter(timeParameter);
        out.organizationFiscalCode("ORGANIZATION_FISCAL_CODE_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.initiativeRewardType(InitiativeRewardType.REFUND);

        TestUtils.checkNotNullFields(out);
        TestUtils.checkNotNullFields(accumulatedAmount);
        TestUtils.checkNotNullFields(timeParameter);
        return out;
    }
}
