package it.gov.pagopa.reward.notification.service.rewards.evaluate;

import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.model.Rewards;
import org.springframework.messaging.Message;
import reactor.core.publisher.Mono;

public interface RewardNotificationRuleEvaluatorService {
    Mono<Rewards> retrieveAndEvaluate(String initiativeId, Reward reward, RewardTransactionDTO trx, Message<String> message);
}
