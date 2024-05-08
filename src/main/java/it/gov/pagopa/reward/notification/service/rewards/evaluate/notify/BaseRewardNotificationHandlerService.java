package it.gov.pagopa.reward.notification.service.rewards.evaluate.notify;

import it.gov.pagopa.reward.notification.dto.mapper.RewardsNotificationMapper;
import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.enums.DepositType;
import it.gov.pagopa.reward.notification.enums.InitiativeRewardType;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public abstract class BaseRewardNotificationHandlerService implements RewardNotificationHandlerService {

    protected final RewardsNotificationRepository rewardsNotificationRepository;
    protected final RewardsNotificationMapper mapper;

    protected BaseRewardNotificationHandlerService(RewardsNotificationRepository rewardsNotificationRepository, RewardsNotificationMapper mapper) {
        this.rewardsNotificationRepository = rewardsNotificationRepository;
        this.mapper = mapper;
    }
    protected Mono<RewardsNotification> createNewNotification(RewardTransactionDTO trx, RewardNotificationRule rule, LocalDate notificationDate, String notificationId) {
        String beneficiaryId = getBeneficiaryId(trx, rule);
        return rewardsNotificationRepository.countByBeneficiaryIdAndInitiativeIdAndOrdinaryIdIsNull(beneficiaryId, rule.getInitiativeId())
                .defaultIfEmpty(0L)
                .map(progressive -> mapper.apply(notificationId, notificationDate, progressive+1, trx, rule));
    }

    protected Mono<RewardsNotification> createNewNotificationWithProgressiveId(RewardTransactionDTO trx, RewardNotificationRule rule, LocalDate notificationDate, String notificationId) {
        return createNewNotification(trx, rule, notificationDate, notificationId)
                .doOnNext(n -> {
                    n.setId(buildNextProgressiveId(n.getId(), n));
                    n.setExternalId(buildNextProgressiveId(n.getExternalId(), n));
                });
    }

    private String buildNextProgressiveId(String n, RewardsNotification n1) {
        return "%s_%d".formatted(n, n1.getProgressive());
    }

    protected void updateReward(RewardTransactionDTO trx, RewardNotificationRule rule, Reward reward, RewardsNotification n) {
        n.setRewardCents(n.getRewardCents() + reward.getAccruedRewardCents());
        n.getTrxIds().add(trx.getId());
        n.setDepositType(calcDepositType(rule, reward));
    }

    public DepositType calcDepositType(RewardNotificationRule rule, Reward reward) {
        return reward.getCounters().isExhaustedBudget() || (rule.getEndDate()!=null && !rule.getEndDate().isAfter(LocalDate.now()))
                ? DepositType.FINAL
                : DepositType.PARTIAL;
    }

    protected String getBeneficiaryId(RewardTransactionDTO trx, RewardNotificationRule rule){
        return rule.getInitiativeRewardType().equals(InitiativeRewardType.REFUND)
                ? trx.getUserId()
                : trx.getMerchantId();
    }
}
