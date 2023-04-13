package it.gov.pagopa.reward.notification.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.reward.notification.dto.controller.detail.RewardNotificationDetailDTO;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.utils.Utils;
import org.apache.commons.lang3.ObjectUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Random;

public class RewardNotificationDetailDTOFaker {

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

    public static RewardNotificationDetailDTO mockInstance(Integer bias){
        return mockInstanceBuilder(bias).build();
    }

    public static RewardNotificationDetailDTO mockInstance(Integer bias, LocalDate date){
        return mockInstanceBuilder(bias, date).build();
    }

    public static RewardNotificationDetailDTO.RewardNotificationDetailDTOBuilder mockInstanceBuilder(Integer bias){
        RewardNotificationDetailDTO.RewardNotificationDetailDTOBuilder out = RewardNotificationDetailDTO.builder();

        bias = ObjectUtils.firstNonNull(bias, getRandomPositiveNumber(null));

        FakeValuesService fakeValuesService = getFakeValuesService(bias);

        out.id("USERID%s_INITIATIVEID%s_%s".formatted(bias, bias, LocalDate.now().format(Utils.FORMATTER_DATE)));
        out.externalId("NOTIFICATIONID%s".formatted(bias));
        out.userId("USERID%s".formatted(bias));
        out.iban("IBAN%s".formatted(bias));
        out.amount(BigDecimal.valueOf(bias*10L));
        out.startDate(LocalDate.now().minusDays(1));
        out.endDate(LocalDate.now());
        out.status(RewardNotificationStatus.EXPORTED);
        out.refundType(Utils.RefundType.ORDINARY);
        out.cro("CRO%s".formatted(fakeValuesService.bothify("?????")));
        out.transferDate(LocalDate.now());
        out.userNotificationDate(LocalDate.now());

        return out;
    }

    public static RewardNotificationDetailDTO.RewardNotificationDetailDTOBuilder mockInstanceBuilder(Integer bias, LocalDate date){
        RewardNotificationDetailDTO.RewardNotificationDetailDTOBuilder out = RewardNotificationDetailDTO.builder();

        bias = ObjectUtils.firstNonNull(bias, getRandomPositiveNumber(null));

        FakeValuesService fakeValuesService = getFakeValuesService(bias);

        out.id("USERID%s_INITIATIVEID%s_%s".formatted(bias, bias, date.format(Utils.FORMATTER_DATE)));
        out.externalId("NOTIFICATIONID%s".formatted(bias));
        out.userId("USERID%s".formatted(bias));
        out.iban("IBAN%s".formatted(bias));
        out.amount(BigDecimal.valueOf(bias*10L));
        out.startDate(date.minusDays(1));
        out.endDate(date);
        out.status(RewardNotificationStatus.EXPORTED);
        out.refundType(Utils.RefundType.ORDINARY);
        out.cro("CRO%s".formatted(fakeValuesService.bothify("?????")));
        out.transferDate(date);
        out.userNotificationDate(date);

        return out;
    }
}
