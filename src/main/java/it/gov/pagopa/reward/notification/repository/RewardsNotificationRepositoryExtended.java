package it.gov.pagopa.reward.notification.repository;

import reactor.core.publisher.Flux;

public interface RewardsNotificationRepositoryExtended {
    Flux<String> findInitiatives2Notify();
}
