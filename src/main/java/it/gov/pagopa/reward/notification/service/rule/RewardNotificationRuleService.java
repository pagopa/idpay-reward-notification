package it.gov.pagopa.reward.notification.service.rule;

import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import reactor.core.publisher.Mono;

public interface RewardNotificationRuleService {
    Mono<RewardNotificationRule> findById(String initiativeId);
    Mono<RewardNotificationRule> save(RewardNotificationRule rewardNotificationRule);
}
