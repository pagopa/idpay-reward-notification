package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface RewardNotificationRuleRepository extends ReactiveMongoRepository<RewardNotificationRule, String> {
}
