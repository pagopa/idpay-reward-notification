package it.gov.pagopa.reward.notification.service.csv;

import it.gov.pagopa.reward.notification.model.RewardsNotification;
import reactor.core.publisher.Mono;

public interface RewardNotificationNotifierService {
    Mono<RewardsNotification> notify(RewardsNotification notification, long deltaRewardCents);
}
