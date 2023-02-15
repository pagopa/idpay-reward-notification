package it.gov.pagopa.reward.notification.service.iban.outcome.recovery;

import it.gov.pagopa.reward.notification.dto.rule.AccumulatedAmountDTO;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.notify.RewardNotificationBudgetExhaustedHandlerServiceImpl;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.notify.RewardNotificationTemporalHandlerServiceImpl;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.notify.RewardNotificationThresholdHandlerServiceImpl;
import it.gov.pagopa.reward.notification.service.rule.RewardNotificationRuleService;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public abstract class BaseDiscardedRewardNotificationServiceImpl implements DiscardedRewardNotificationService{

    private final RewardNotificationRuleService notificationRuleService;


    // handlers to calculate notificationDate
    private final RewardNotificationTemporalHandlerServiceImpl temporalHandler;
    private final RewardNotificationBudgetExhaustedHandlerServiceImpl budgetExhaustedHandler;
    private final RewardNotificationThresholdHandlerServiceImpl thresholdHandler;

    protected BaseDiscardedRewardNotificationServiceImpl(RewardNotificationRuleService notificationRuleService,
                                                         RewardNotificationTemporalHandlerServiceImpl temporalHandler,
                                                         RewardNotificationBudgetExhaustedHandlerServiceImpl budgetExhaustedHandler,
                                                         RewardNotificationThresholdHandlerServiceImpl thresholdHandler) {
        this.notificationRuleService = notificationRuleService;
        this.temporalHandler = temporalHandler;
        this.budgetExhaustedHandler = budgetExhaustedHandler;
        this.thresholdHandler = thresholdHandler;
    }

    @Override
    public Mono<RewardsNotification> setRemedialNotificationDate(String initiativeId, RewardsNotification notification) {

        return notificationRuleService.findById(initiativeId)
                .map(rule -> {
                    LocalDate today = LocalDate.now();

                    if (rule.getEndDate() != null && rule.getEndDate().isBefore(today)) {
                        return budgetExhaustedHandler.calculateNotificationDate();
                    } else if(rule.getTimeParameter() != null){
                        return temporalHandler.calculateNotificationDate(today, rule);
                    } else if(rule.getAccumulatedAmount() != null){
                        if(AccumulatedAmountDTO.AccumulatedTypeEnum.BUDGET_EXHAUSTED.equals(rule.getAccumulatedAmount().getAccumulatedType())){
                            return budgetExhaustedHandler.calculateNotificationDate();
                        } else if(rule.getAccumulatedAmount().getRefundThreshold() != null)  {
                            return thresholdHandler.calculateNotificationDate();
                        } else {
                            throw new IllegalStateException("Not valid threshold rule %s".formatted(rule.getInitiativeId()));
                        }
                    } else {
                        throw new IllegalStateException("Not valid rule %s".formatted(rule.getInitiativeId()));
                    }
                })
                .map(date -> {
                    notification.setNotificationDate(date);
                    return notification;
                });
    }
}
