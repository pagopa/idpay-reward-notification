package it.gov.pagopa.reward.notification.service.iban.outcome.recovery;

import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import reactor.core.publisher.Mono;

public interface DiscardedRewardNotificationService {

    Mono<RewardsNotification> setRemedialNotificationDate(RewardsNotification notification);
    Mono<RewardsNotification> setRemedialNotificationDate(RewardNotificationRule notificationRule, RewardsNotification notification);
}
