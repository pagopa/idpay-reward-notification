package it.gov.pagopa.reward.notification.service.rewards.evaluate.notify;

import it.gov.pagopa.reward.notification.dto.mapper.RewardsNotificationMapper;
import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class RewardNotificationThresholdHandlerServiceImpl extends BaseRewardNotificationHandlerService implements RewardNotificationHandlerService {

    public RewardNotificationThresholdHandlerServiceImpl(RewardsNotificationRepository rewardsNotificationRepository, RewardsNotificationMapper mapper) {
        super(rewardsNotificationRepository, mapper);
    }

    @Override
    public Mono<RewardsNotification> handle(RewardTransactionDTO trx, RewardNotificationRule rule, Reward reward) {
        return Mono.just(RewardsNotification.builder()
                .id("%s_%s_THRESHOLD_NOTIFICATIONID".formatted(rule.getInitiativeId(), trx.getUserId()))
                .trxIds(List.of(trx.getId()))
                .build()); //TODO
    }

}
