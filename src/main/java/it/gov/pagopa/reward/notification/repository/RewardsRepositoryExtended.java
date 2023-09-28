package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.model.Rewards;
import reactor.core.publisher.Flux;

public interface RewardsRepositoryExtended {
    Flux<Rewards> findByInitiativeIdWithBatch(String initiativeId, int batchSize);
}
