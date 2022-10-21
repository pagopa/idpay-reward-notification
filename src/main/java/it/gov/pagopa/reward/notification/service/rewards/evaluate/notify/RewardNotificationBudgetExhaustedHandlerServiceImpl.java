package it.gov.pagopa.reward.notification.service.rewards.evaluate.notify;

import it.gov.pagopa.reward.notification.dto.mapper.RewardsNotificationMapper;
import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.enums.DepositType;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RewardNotificationBudgetExhaustedHandlerServiceImpl extends RewardNotificationThresholdHandlerServiceImpl implements RewardNotificationHandlerService {

    public RewardNotificationBudgetExhaustedHandlerServiceImpl(
            @Value("${app.rewards-notification.threshold-notification-day}") String notificationDay,
            RewardsNotificationRepository rewardsNotificationRepository, RewardsNotificationMapper mapper) {
        super(notificationDay, rewardsNotificationRepository, mapper);
    }

    @Override
    protected boolean isThresholdReached(RewardNotificationRule rule, RewardsNotification n, Reward reward) {
        return reward.getCounters() != null && reward.getCounters().isExhaustedBudget();
    }

    @Override
    public DepositType calcDepositType(RewardNotificationRule rule, Reward reward) {
        return DepositType.FINAL;
    }
}
