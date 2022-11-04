package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.model.RewardsNotification;
import reactor.core.publisher.Flux;

import java.util.Collection;

public interface RewardsNotificationRepositoryExtended {
    Flux<String> findInitiatives2Notify(Collection<String> initiativeIds2Exclude);
    Flux<RewardsNotification> findRewards2Notify(String initiativeId);
}
