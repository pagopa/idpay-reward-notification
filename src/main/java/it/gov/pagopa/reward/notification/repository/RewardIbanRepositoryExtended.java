package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.model.RewardIban;
import reactor.core.publisher.Flux;

public interface RewardIbanRepositoryExtended {
    Flux<RewardIban> findByInitiativeIdWithBatch(String initiativeId, int batchSize);
}
