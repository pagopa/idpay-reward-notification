package it.gov.pagopa.reward.notification.service.rewards.evaluate.notify;

import it.gov.pagopa.reward.notification.dto.rule.AccumulatedAmountDTO;
import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.dto.trx.RewardCounters;
import it.gov.pagopa.reward.notification.enums.DepositType;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;

import java.math.BigDecimal;

class RewardNotificationBudgetExhaustedHandlerServiceImplTest extends BaseRewardNotificationThresholdHandlerTest {

    @Override
    protected RewardNotificationBudgetExhaustedHandlerServiceImpl buildService(String notificationDay){
        return new RewardNotificationBudgetExhaustedHandlerServiceImpl(notificationDay, repositoryMock, mapperSpy);
    }

    @Override
    protected RewardNotificationRule buildRule() {
        RewardNotificationRule rule = new RewardNotificationRule();
        rule.setInitiativeId("INITIATIVEID");
        rule.setAccumulatedAmount(new AccumulatedAmountDTO());
        rule.getAccumulatedAmount().setAccumulatedType(AccumulatedAmountDTO.AccumulatedTypeEnum.BUDGET_EXHAUSTED);
        return rule;
    }

    @Override
    protected DepositType getExpectedDepositType() {
        return DepositType.FINAL;
    }

    @Override
    protected Reward testHandleNewNotifyRefundWithFutureNotification_buildOverFlowingReward(boolean isStillOverflowing) {
        Reward reward = new Reward(BigDecimal.valueOf(isStillOverflowing ? 0 : -3));
        reward.setCounters(new RewardCounters());
        reward.getCounters().setExhaustedBudget(isStillOverflowing);
        return reward;
    }

    @Override
    protected long testHandleNewNotifyRefundWithFutureNotification_expectedReward(boolean isStillOverflowing) {
        return isStillOverflowing ? 600L : 300L;
    }

    @Override
    protected Reward testHandleUpdateNotifyOverflowingThreshold_buildOverFlowingReward() {
        Reward reward = new Reward(BigDecimal.valueOf(3));
        reward.setCounters(new RewardCounters());
        reward.getCounters().setExhaustedBudget(true);
        return reward;
    }
}
