package it.gov.pagopa.reward.notification.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.notification.dto.rule.*;
import it.gov.pagopa.reward.notification.enums.InitiativeRewardType;
import org.apache.commons.lang3.ObjectUtils;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Random;

public class InitiativeRefundDTOFaker {
    private static final Random randomGenerator = new Random();

    public static Random getRandom(Integer bias) {
        return bias == null ? randomGenerator : new Random(bias);
    }

    public static int getRandomPositiveNumber(Integer bias) {
        return Math.abs(getRandom(bias).nextInt());
    }

    private static final FakeValuesService fakeValuesServiceGlobal = new FakeValuesService(new Locale("it"), new RandomService(null));

    public static FakeValuesService getFakeValuesService(Integer bias) {
        return bias == null ? fakeValuesServiceGlobal : new FakeValuesService(new Locale("it"), new RandomService(getRandom(bias)));
    }

    /** It will return an example of {@link InitiativeRefund2StoreDTO}. Providing a bias, it will return a pseudo-casual object */
    public static InitiativeRefund2StoreDTO mockInstance(Integer bias){
        return mockInstanceBuilder(bias).build();
    }

    /** It will return an example of builder to obtain a {@link InitiativeRefund2StoreDTO}. Providing a bias, it will return a pseudo-casual object */
    public static InitiativeRefund2StoreDTO.InitiativeRefund2StoreDTOBuilder mockInstanceBuilder(Integer bias){
        InitiativeRefund2StoreDTO.InitiativeRefund2StoreDTOBuilder out = InitiativeRefund2StoreDTO.builder();

        bias = ObjectUtils.firstNonNull(bias, getRandomPositiveNumber(null));

        FakeValuesService fakeValuesService = getFakeValuesService(bias);

        out.initiativeId("ID_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.initiativeName("NAME_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.organizationId("ORGANIZATION_ID_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.organizationVat("ORGANIZATION_VAT_%d_%s".formatted(bias, fakeValuesService.bothify("???")));

        InitiativeGeneralDTO initiativeGeneral = InitiativeGeneralDTO
                .builder()
                .endDate(LocalDate.now())
                .build();

        out.general(initiativeGeneral);

        AccumulatedAmountDTO accumulatedAmount = AccumulatedAmountDTO.builder()
                .accumulatedType(AccumulatedAmountDTO.AccumulatedTypeEnum.THRESHOLD_REACHED)
                .refundThresholdCents(10000L)
                .build();
        TimeParameterDTO timeParameter = TimeParameterDTO.builder()
                .timeType(TimeParameterDTO.TimeTypeEnum.DAILY)
                .build();
        InitiativeRefundRuleDTO initiativeRefundRule = InitiativeRefundRuleDTO.builder()
                .accumulatedAmount(accumulatedAmount)
                .timeParameter(timeParameter)
                .build();
        out.refundRule(initiativeRefundRule);
        out.initiativeRewardType(InitiativeRewardType.REFUND);

        TestUtils.checkNotNullFields(out);
        TestUtils.checkNotNullFields(accumulatedAmount, "refundThresholdCents");
        TestUtils.checkNotNullFields(timeParameter);
        return out;
    }

}
