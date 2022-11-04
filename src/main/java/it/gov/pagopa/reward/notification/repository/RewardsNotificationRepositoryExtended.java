package it.gov.pagopa.reward.notification.repository;

import reactor.core.publisher.Flux;

import java.util.Collection;

public interface RewardsNotificationRepositoryExtended {
    Flux<String> findInitiatives2Notify(Collection<String> initiativeIds2Exclude);
}
