package it.gov.pagopa.reward.notification.service.csv;

import it.gov.pagopa.reward.notification.model.RewardsNotification;
import reactor.core.publisher.Mono;

public interface RewardNotificationErrorNotifierService {
    Mono<RewardsNotification> notify(RewardsNotification reward);
}
