package it.gov.pagopa.reward.notification.service.rewards.evaluate;

import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
public class RewardNotificationUpdateServiceImpl implements RewardNotificationUpdateService {
    @Override
    public Mono<RewardsNotification> configureRewardNotification(RewardTransactionDTO trx, RewardNotificationRule rule, Reward reward) {
        log.trace("[REWARD_NOTIFICATION] Configuring reward notification {}_{}", trx.getId(), rule.getInitiativeId())
        ;
        return Mono.just(RewardsNotification.builder()
                .id("%s_%s_NOTIFICATIONID".formatted(rule.getInitiativeId(), trx.getUserId()))
                        .trxIds(List.of(trx.getId()))
                .build()); //TODO
    }
}
