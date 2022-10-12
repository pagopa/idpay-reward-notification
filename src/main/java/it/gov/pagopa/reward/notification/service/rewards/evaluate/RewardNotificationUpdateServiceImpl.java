package it.gov.pagopa.reward.notification.service.rewards.evaluate;

import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class RewardNotificationUpdateServiceImpl implements RewardNotificationUpdateService {
    @Override
    public Mono<String> configureRewardNotification(RewardTransactionDTO trx, RewardNotificationRule rule, Reward reward) {
        log.trace("[REWARD_NOTIFICATION] Configuring reward notification {}_{}", trx.getId(), rule.getInitiativeId())
        ;
        return Mono.just("%s_%s_NOTIFICATIONID".formatted(rule.getInitiativeId(), trx.getUserId())); //TODO
    }
}
