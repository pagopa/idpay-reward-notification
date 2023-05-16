package it.gov.pagopa.reward.notification.service.rewards.evaluate.notify;

import it.gov.pagopa.reward.notification.dto.rule.AccumulatedAmountDTO;
import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.enums.DepositType;
import it.gov.pagopa.reward.notification.enums.InitiativeRewardType;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;

import java.math.BigDecimal;

class RewardNotificationThresholdHandlerServiceImplTest extends BaseRewardNotificationThresholdHandlerTest {

    @Override
    protected BaseRewardNotificationThresholdBasedHandler buildService(String notificationDay) {
        return new RewardNotificationThresholdHandlerServiceImpl(notificationDay, repositoryMock, mapperSpy);
    }

    @Override
    protected RewardNotificationRule buildRule() {
        RewardNotificationRule rule = new RewardNotificationRule();
        rule.setInitiativeId("INITIATIVEID");
        rule.setAccumulatedAmount(new AccumulatedAmountDTO());
        rule.getAccumulatedAmount().setAccumulatedType(AccumulatedAmountDTO.AccumulatedTypeEnum.THRESHOLD_REACHED);
        rule.getAccumulatedAmount().setRefundThresholdCents(500L);
        rule.setInitiativeRewardType(InitiativeRewardType.REFUND.name());
        return rule;
    }

    @Override
    protected DepositType getExpectedDepositType() {
        return DepositType.PARTIAL;
    }

    @Override
    protected Reward testHandleNewNotifyRefundWithFutureNotification_buildOverFlowingReward(boolean isStillOverflowing) {
        return new Reward(BigDecimal.valueOf(isStillOverflowing ? -0.5 : -3));
    }

    @Override
    protected long testHandleNewNotifyRefundWithFutureNotification_expectedReward(boolean isStillOverflowing) {
        return isStillOverflowing ? 550L : 300L;
    }

    @Override
    protected Reward testHandleUpdateNotifyOverflowingThreshold_buildOverFlowingReward() {
        return new Reward(BigDecimal.valueOf(3));
    }
}
