package it.gov.pagopa.reward.notification.utils;

import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;

import java.util.Arrays;
import java.util.List;

public final class NotificationConstants {

    private NotificationConstants() {}

    public static final List<RewardNotificationStatus> REWARD_NOTIFICATION_EXPOSED_STATUS = Arrays.asList(
            RewardNotificationStatus.EXPORTED,
            RewardNotificationStatus.COMPLETED_OK,
            RewardNotificationStatus.COMPLETED_KO,
            RewardNotificationStatus.RECOVERED
    );
}

