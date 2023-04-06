package it.gov.pagopa.reward.notification.service.iban.outcome.recovery;

import it.gov.pagopa.reward.notification.dto.rule.AccumulatedAmountDTO;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.notify.RewardNotificationBudgetExhaustedHandlerServiceImpl;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.notify.RewardNotificationTemporalHandlerServiceImpl;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.notify.RewardNotificationThresholdHandlerServiceImpl;
import it.gov.pagopa.reward.notification.service.rule.RewardNotificationRuleService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Primary
@Service
public class BaseDiscardedRewardNotificationServiceImpl implements DiscardedRewardNotificationService{

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
    public Mono<RewardsNotification> setRemedialNotificationDate(RewardsNotification notification) {

        return notificationRuleService.findById(notification.getInitiativeId())
                .map(this::getNotificationDate)
                .map(date -> {
                    notification.setNotificationDate(date);
                    return notification;
                });
    }

    @Override
    public Mono<RewardsNotification> setRemedialNotificationDate(RewardNotificationRule notificationRule, RewardsNotification notification) {

        return Mono.just(notificationRule)
                .map(this::getNotificationDate)
                .map(date -> {
                    notification.setNotificationDate(date);
                    return notification;
                });
    }



    private LocalDate getNotificationDate(RewardNotificationRule r) {
        LocalDate today = LocalDate.now();

        if (r.getEndDate() != null && r.getEndDate().isBefore(today)) {
            return budgetExhaustedHandler.calculateNotificationDate();
        } else if(r.getTimeParameter() != null){
            return temporalHandler.calculateNotificationDate(today, r);
        } else if(r.getAccumulatedAmount() != null){
            if(AccumulatedAmountDTO.AccumulatedTypeEnum.BUDGET_EXHAUSTED.equals(r.getAccumulatedAmount().getAccumulatedType())){
                return budgetExhaustedHandler.calculateNotificationDate();
            } else if(r.getAccumulatedAmount().getRefundThreshold() != null)  {
                return thresholdHandler.calculateNotificationDate();
            } else {
                throw new IllegalStateException("Not valid threshold rule %s".formatted(r.getInitiativeId()));
            }
        } else {
            throw new IllegalStateException("Not valid rule %s".formatted(r.getInitiativeId()));
        }
    }
}
