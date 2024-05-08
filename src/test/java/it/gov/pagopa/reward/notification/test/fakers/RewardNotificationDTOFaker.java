package it.gov.pagopa.reward.notification.test.fakers;

import it.gov.pagopa.reward.notification.dto.controller.detail.RewardNotificationDTO;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Random;

public class RewardNotificationDTOFaker {

    private static final Random randomGenerator = new Random();

    public static Random getRandom(Integer bias) {
        return bias == null ? randomGenerator : new Random(bias);
    }

    public static int getRandomPositiveNumber(Integer bias) {
        return Math.abs(getRandom(bias).nextInt());
    }

    public static RewardNotificationDTO mockInstance(Integer bias){
        return mockInstanceBuilder(bias).build();
    }

    public static RewardNotificationDTO.RewardNotificationDTOBuilder mockInstanceBuilder(Integer bias){
        RewardNotificationDTO.RewardNotificationDTOBuilder out = RewardNotificationDTO.builder();

        bias = ObjectUtils.firstNonNull(bias, getRandomPositiveNumber(null));

        out.iban("IBAN%s".formatted(bias));
        out.amountCents(bias*10L);
        out.status(RewardNotificationStatus.EXPORTED);

        return out;
    }
}
