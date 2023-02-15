package it.gov.pagopa.reward.notification.service.iban.outcome.recovery;

import it.gov.pagopa.reward.notification.model.RewardsNotification;
import reactor.core.publisher.Mono;

public interface DiscardedRewardNotificationService {

    Mono<RewardsNotification> setRemedialNotificationDate(String initiativeId, RewardsNotification notification);
}
