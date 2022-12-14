package it.gov.pagopa.reward.notification.service.rewards.evaluate;

import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import reactor.core.publisher.Mono;

public interface RewardNotificationHandlerFacadeService {
    Mono<RewardsNotification> configureRewardNotification(RewardTransactionDTO trx, RewardNotificationRule rule, Reward reward);
}
