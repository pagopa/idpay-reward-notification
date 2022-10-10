package it.gov.pagopa.reward.notification.service.rewards;

import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class RewardNotificationUpdateServiceImpl implements RewardNotificationUpdateService {
    @Override
    public Mono<String> configureRewardNotification(RewardTransactionDTO trx, RewardNotificationRule rule, Reward reward) {
        return Mono.just("NOTIFICATIONID"); //TODO
    }
}
