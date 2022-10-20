package it.gov.pagopa.reward.notification.service.rewards.evaluate.notify;

import it.gov.pagopa.reward.notification.dto.mapper.RewardsNotificationMapper;
import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.enums.DepositType;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import org.springframework.data.domain.Example;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

public abstract class BaseRewardNotificationHandlerService implements RewardNotificationHandlerService {

    protected final RewardsNotificationRepository rewardsNotificationRepository;
    protected final RewardsNotificationMapper mapper;

    protected BaseRewardNotificationHandlerService(RewardsNotificationRepository rewardsNotificationRepository, RewardsNotificationMapper mapper) {
        this.rewardsNotificationRepository = rewardsNotificationRepository;
        this.mapper = mapper;
    }

    protected Mono<RewardsNotification> createNewNotification(RewardTransactionDTO trx, RewardNotificationRule rule, LocalDate notificationDate, String notificationId) {
        RewardsNotification query = new RewardsNotification();
        query.setUserId(trx.getUserId());
        query.setInitiativeId(rule.getInitiativeId());
        query.setTrxIds(null);

        return rewardsNotificationRepository.count(Example.of(query))
                .defaultIfEmpty(0L)
                .map(progressive -> mapper.apply(notificationId, notificationDate, progressive+1, trx, rule));
    }

    protected void updateReward(RewardTransactionDTO trx, RewardNotificationRule rule, Reward reward, RewardsNotification n) {
        n.setRewardCents(n.getRewardCents() + reward.getAccruedReward().multiply(BigDecimal.valueOf(100L)).longValue());
        n.getTrxIds().add(trx.getId());
        n.setDepositType(calcDepositType(rule, reward));
    }

    public DepositType calcDepositType(RewardNotificationRule rule, Reward reward) {
        return reward.getCounters().isExhaustedBudget() || (rule.getEndDate()!=null && !rule.getEndDate().isAfter(LocalDate.now()))
                ? DepositType.FINAL
                : DepositType.PARTIAL;
    }
}
