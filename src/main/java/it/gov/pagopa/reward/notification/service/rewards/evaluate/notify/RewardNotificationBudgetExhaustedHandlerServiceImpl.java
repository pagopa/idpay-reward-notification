package it.gov.pagopa.reward.notification.service.rewards.evaluate.notify;

import it.gov.pagopa.reward.notification.dto.mapper.RewardsNotificationMapper;
import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.enums.DepositType;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class RewardNotificationBudgetExhaustedHandlerServiceImpl extends BaseRewardNotificationHandlerService implements RewardNotificationHandlerService {

    public RewardNotificationBudgetExhaustedHandlerServiceImpl(RewardsNotificationRepository rewardsNotificationRepository, RewardsNotificationMapper mapper) {
        super(rewardsNotificationRepository, mapper);
    }

    @Override
    public Mono<RewardsNotification> handle(RewardTransactionDTO trx, RewardNotificationRule rule, Reward reward) {
        return Mono.just(RewardsNotification.builder()
                .id("%s_%s_BUDGET_EXHAUSTED_NOTIFICATIONID".formatted(rule.getInitiativeId(), trx.getUserId()))
                .userId(trx.getUserId())
                .trxIds(List.of(trx.getId()))
                .build()); //TODO
    }

    @Override
    public DepositType calcDepositType(RewardNotificationRule rule, Reward reward) {
        return DepositType.FINAL;
    }
}
