package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import reactor.core.publisher.Flux;

public interface RewardNotificationRuleRepositoryExtended {
    Flux<RewardNotificationRule> findByIdWithBatch(String initiativeId, int batchSize);
}
