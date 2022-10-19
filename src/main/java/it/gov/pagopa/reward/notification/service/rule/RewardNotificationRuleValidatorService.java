package it.gov.pagopa.reward.notification.service.rule;

import it.gov.pagopa.reward.notification.model.RewardNotificationRule;

/** It will validate reward notification rule */
public interface RewardNotificationRuleValidatorService {
    void validate(RewardNotificationRule rule);
}
