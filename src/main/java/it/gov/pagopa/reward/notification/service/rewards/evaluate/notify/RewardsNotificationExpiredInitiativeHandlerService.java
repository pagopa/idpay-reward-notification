package it.gov.pagopa.reward.notification.service.rewards.evaluate.notify;

import it.gov.pagopa.reward.notification.model.RewardsNotification;
import reactor.core.publisher.Flux;

public interface RewardsNotificationExpiredInitiativeHandlerService {

    Flux<RewardsNotification> handle();
}
