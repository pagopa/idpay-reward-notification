package it.gov.pagopa.reward.notification.service.rule;

import it.gov.pagopa.reward.notification.dto.rule.AccumulatedAmountDTO;
import it.gov.pagopa.reward.notification.dto.rule.TimeParameterDTO;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import org.springframework.stereotype.Service;

@Service
public class RewardNotificationRuleValidatorServiceImpl implements RewardNotificationRuleValidatorService {
    @Override
    public void validate(RewardNotificationRule rule) {
        TimeParameterDTO timeParameter = rule.getTimeParameter();
        AccumulatedAmountDTO accumulatedAmount = rule.getAccumulatedAmount();

        if (timeParameter == null && accumulatedAmount == null) {
            throw new IllegalArgumentException("[REWARD_NOTIFICATION_RULE] [INVALID_RULE] Invalid rule %s".formatted(rule));
        }

        if (timeParameter != null) {
            if (timeParameter.getTimeType() == null) {
                throw new IllegalArgumentException("[REWARD_NOTIFICATION_RULE] [INVALID_RULE] Invalid time rule %s".formatted(rule));
            }
            if(TimeParameterDTO.TimeTypeEnum.CLOSED.equals(timeParameter.getTimeType()) && rule.getEndDate()==null){
                throw new IllegalArgumentException("[REWARD_NOTIFICATION_RULE] [INVALID_RULE] Invalid time closed rule %s".formatted(rule));
            }
        }

        if (accumulatedAmount != null) {
            if (accumulatedAmount.getAccumulatedType() == null) {
                throw new IllegalArgumentException("[REWARD_NOTIFICATION_RULE] [INVALID_RULE] Invalid accumulated rule %s".formatted(rule));
            } else if (
                    AccumulatedAmountDTO.AccumulatedTypeEnum.THRESHOLD_REACHED.equals(accumulatedAmount.getAccumulatedType()) &&
                            accumulatedAmount.getRefundThresholdCents() == null) {
                throw new IllegalArgumentException("[REWARD_NOTIFICATION_RULE] [INVALID_RULE] Invalid threshold rule %s".formatted(rule));
            }
        }
    }
}
