package it.gov.pagopa.reward.notification.service;

import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import reactor.core.publisher.Mono;

public interface RewardNotificationRuleService {
    Mono<RewardNotificationRule> save(RewardNotificationRule rewardNotificationRule);
}
