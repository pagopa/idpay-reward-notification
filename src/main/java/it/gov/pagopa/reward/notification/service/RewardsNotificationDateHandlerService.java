package it.gov.pagopa.reward.notification.service;

import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import reactor.core.publisher.Mono;

public interface RewardsNotificationDateHandlerService {

    Mono<RewardsNotification> setHandledNotificationDate(RewardsNotification notification);
    Mono<RewardsNotification> setHandledNotificationDate(RewardNotificationRule initiative, RewardsNotification notification);
}
