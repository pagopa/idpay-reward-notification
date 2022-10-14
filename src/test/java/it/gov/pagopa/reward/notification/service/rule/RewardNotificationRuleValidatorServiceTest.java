package it.gov.pagopa.reward.notification.service.rule;

import it.gov.pagopa.reward.notification.dto.rule.AccumulatedAmountDTO;
import it.gov.pagopa.reward.notification.dto.rule.TimeParameterDTO;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RewardNotificationRuleValidatorServiceTest {

    private final RewardNotificationRuleValidatorService validatorService = new RewardNotificationRuleValidatorServiceImpl();

    @Test
    void testInvalidRule(){
        RewardNotificationRule rule = new RewardNotificationRule();

        test(rule, "[REWARD_NOTIFICATION_RULE] [INVALID_RULE] Invalid rule %s");
    }

    @Test
    void testInvalidTimeRule(){
        RewardNotificationRule rule = new RewardNotificationRule();
        rule.setTimeParameter(new TimeParameterDTO());

        test(rule, "[REWARD_NOTIFICATION_RULE] [INVALID_RULE] Invalid time rule %s");
    }

    @Test
    void testInvalidAccumulatedRule(){
        RewardNotificationRule rule = new RewardNotificationRule();
        rule.setAccumulatedAmount(new AccumulatedAmountDTO());

        test(rule, "[REWARD_NOTIFICATION_RULE] [INVALID_RULE] Invalid accumulated rule %s");
    }

    @Test
    void testInvalidThresholdRule(){
        RewardNotificationRule rule = new RewardNotificationRule();
        rule.setAccumulatedAmount(new AccumulatedAmountDTO());
        rule.getAccumulatedAmount().setAccumulatedType(AccumulatedAmountDTO.AccumulatedTypeEnum.THRESHOLD_REACHED);

        test(rule, "[REWARD_NOTIFICATION_RULE] [INVALID_RULE] Invalid threshold rule %s");
    }

    private void test(RewardNotificationRule rule, String expected) {
        try{
            validatorService.validate(rule);
            Assertions.fail("Expected exception");
        } catch (IllegalArgumentException e){
            Assertions.assertEquals(expected.formatted(rule), e.getMessage());
        }
    }
}
