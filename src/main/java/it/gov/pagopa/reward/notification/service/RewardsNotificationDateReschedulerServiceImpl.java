package it.gov.pagopa.reward.notification.service;

import it.gov.pagopa.reward.notification.dto.rule.AccumulatedAmountDTO;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.notify.RewardNotificationBudgetExhaustedHandlerServiceImpl;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.notify.RewardNotificationTemporalHandlerServiceImpl;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.notify.RewardNotificationThresholdHandlerServiceImpl;
import it.gov.pagopa.reward.notification.service.rule.RewardNotificationRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Service
@Slf4j
public class RewardsNotificationDateReschedulerServiceImpl implements RewardsNotificationDateReschedulerService {

    private final RewardNotificationRuleService notificationRuleService;


    // handlers to calculate notificationDate
    private final RewardNotificationTemporalHandlerServiceImpl temporalHandler;
    private final RewardNotificationBudgetExhaustedHandlerServiceImpl budgetExhaustedHandler;
    private final RewardNotificationThresholdHandlerServiceImpl thresholdHandler;

    public RewardsNotificationDateReschedulerServiceImpl(RewardNotificationRuleService notificationRuleService,
                                                         RewardNotificationTemporalHandlerServiceImpl temporalHandler,
                                                         RewardNotificationBudgetExhaustedHandlerServiceImpl budgetExhaustedHandler,
                                                         RewardNotificationThresholdHandlerServiceImpl thresholdHandler) {
        this.notificationRuleService = notificationRuleService;
        this.temporalHandler = temporalHandler;
        this.budgetExhaustedHandler = budgetExhaustedHandler;
        this.thresholdHandler = thresholdHandler;
    }

    @Override
    public Mono<RewardsNotification> setHandledNotificationDate(RewardsNotification notification) {
        return setHandledNotificationDate(null, notification);
    }

    @Override
    public Mono<RewardsNotification> setHandledNotificationDate(RewardNotificationRule initiative, RewardsNotification notification) {

        return Mono.justOrEmpty(initiative)
                .switchIfEmpty(notificationRuleService.findById(notification.getInitiativeId()))
                .map(r -> {
                    notification.setNotificationDate(getNotificationDate(r));
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
            } else if(r.getAccumulatedAmount().getRefundThresholdCents() != null)  {
                return thresholdHandler.calculateNotificationDate();
            } else {
                throw new IllegalStateException("Not valid threshold rule %s".formatted(r.getInitiativeId()));
            }
        } else {
            throw new IllegalStateException("Not valid rule %s".formatted(r.getInitiativeId()));
        }
    }
}
