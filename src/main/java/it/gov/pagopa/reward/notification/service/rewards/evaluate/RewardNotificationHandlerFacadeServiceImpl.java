package it.gov.pagopa.reward.notification.service.rewards.evaluate;

import it.gov.pagopa.reward.notification.dto.rule.AccumulatedAmountDTO;
import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.notify.RewardNotificationBudgetExhaustedHandlerServiceImpl;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.notify.RewardNotificationHandlerService;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.notify.RewardNotificationTemporalHandlerServiceImpl;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.notify.RewardNotificationThresholdHandlerServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class RewardNotificationHandlerFacadeServiceImpl implements RewardNotificationHandlerFacadeService {

    private final RewardNotificationTemporalHandlerServiceImpl temporalHandler;
    private final RewardNotificationBudgetExhaustedHandlerServiceImpl budgetExhaustedHandler;
    private final RewardNotificationThresholdHandlerServiceImpl thresholdHandler;

    public RewardNotificationHandlerFacadeServiceImpl(RewardNotificationTemporalHandlerServiceImpl temporalHandler, RewardNotificationBudgetExhaustedHandlerServiceImpl budgetExhaustedHandler, RewardNotificationThresholdHandlerServiceImpl thresholdHandler) {
        this.temporalHandler = temporalHandler;
        this.budgetExhaustedHandler = budgetExhaustedHandler;
        this.thresholdHandler = thresholdHandler;
    }

    @Override
    public Mono<RewardsNotification> configureRewardNotification(RewardTransactionDTO trx, RewardNotificationRule rule, Reward reward) {
        log.trace("[REWARD_NOTIFICATION] Configuring reward notification {}_{}", trx.getId(), rule.getInitiativeId());

        RewardNotificationHandlerService handler;

        if(rule.getTimeParameter() != null){
            handler = temporalHandler;
        } else if(rule.getAccumulatedAmount() != null){
            if(AccumulatedAmountDTO.AccumulatedTypeEnum.BUDGET_EXHAUSTED.equals(rule.getAccumulatedAmount().getAccumulatedType())){
                handler = budgetExhaustedHandler;
            } else if(rule.getAccumulatedAmount().getRefundThreshold() != null)  {
                handler = thresholdHandler;
            } else {
                return Mono.error(new IllegalStateException("[REWARD_NOTIFICATION] [INVALID_INITIATIVE] Not valid threshold rule %s".formatted(rule.getInitiativeId())));
            }
        } else {
            return Mono.error(new IllegalStateException("[REWARD_NOTIFICATION] [INVALID_INITIATIVE] Not valid rule %s".formatted(rule.getInitiativeId())));
        }

        return handler.handle(trx, rule, reward);
    }
}
