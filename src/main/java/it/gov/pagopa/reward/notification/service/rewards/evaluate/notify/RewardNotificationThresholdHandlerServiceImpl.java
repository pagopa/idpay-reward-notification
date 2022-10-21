package it.gov.pagopa.reward.notification.service.rewards.evaluate.notify;

import it.gov.pagopa.reward.notification.dto.mapper.RewardsNotificationMapper;
import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RewardNotificationThresholdHandlerServiceImpl extends BaseRewardNotificationThresholdBasedHandler implements RewardNotificationHandlerService {


    public RewardNotificationThresholdHandlerServiceImpl(
            @Value("${app.rewards-notification.threshold-notification-day}") String notificationDay,
            RewardsNotificationRepository rewardsNotificationRepository, RewardsNotificationMapper mapper) {
        super(notificationDay, rewardsNotificationRepository, mapper);
    }

    @Override
    protected boolean isThresholdReached(RewardNotificationRule rule, RewardsNotification n, Reward reward) {
        return n.getRewardCents() >= rule.getAccumulatedAmount().getRefundThresholdCents();
    }

}
