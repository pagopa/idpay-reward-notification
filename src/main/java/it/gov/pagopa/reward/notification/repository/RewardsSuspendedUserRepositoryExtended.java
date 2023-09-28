package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.model.RewardSuspendedUser;
import reactor.core.publisher.Flux;

public interface RewardsSuspendedUserRepositoryExtended {
    Flux<RewardSuspendedUser> findByInitiativeIdWithBatch(String initiativeId, int batchSize);
}
