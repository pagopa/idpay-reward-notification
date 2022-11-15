package it.gov.pagopa.reward.notification.test.fakers;

import it.gov.pagopa.reward.notification.dto.mapper.RewardsNotificationMapper;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.utils.Utils;

import java.time.LocalDate;

public class RewardsNotificationFaker {

    private final static RewardsNotificationMapper mapper = new RewardsNotificationMapper();

    /**
     * @see #mockInstance(Integer) using INITIATIVEID
     */
    public static RewardsNotification mockInstance(Integer bias) {
        return mockInstanceBuilder(bias).build();
    }

    /**
     * It will return an example of {@link RewardsNotification}. Providing a bias, it will return a pseudo-casual object
     */
    public static RewardsNotification mockInstance(Integer bias, String initiativeId, LocalDate notificationDate) {
        return mockInstanceBuilder(bias, initiativeId, notificationDate).build();
    }

    public static RewardsNotification.RewardsNotificationBuilder mockInstanceBuilder(Integer bias) {
        return mockInstanceBuilder(bias, "INITIATIVEID", LocalDate.now());
    }
    public static RewardsNotification.RewardsNotificationBuilder mockInstanceBuilder(Integer bias, String initiativeId, LocalDate notificationDate) {
        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(bias);
        RewardNotificationRule rule = RewardNotificationRuleFaker.mockInstance(bias);
        rule.setInitiativeId(initiativeId);
        String notificationId = "%s_%s_%s".formatted(trx.getUserId(), initiativeId, notificationDate!=null?notificationDate.format(Utils.FORMATTER_DATE):"null");
        return mapper.apply(notificationId, notificationDate, 0L, trx, rule).toBuilder();
    }
}
