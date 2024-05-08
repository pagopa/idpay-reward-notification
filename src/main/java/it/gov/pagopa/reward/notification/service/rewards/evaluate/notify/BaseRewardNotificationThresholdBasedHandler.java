package it.gov.pagopa.reward.notification.service.rewards.evaluate.notify;

import it.gov.pagopa.reward.notification.dto.mapper.RewardsNotificationMapper;
import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Slf4j
public abstract class BaseRewardNotificationThresholdBasedHandler extends BaseRewardNotificationHandlerService implements RewardNotificationHandlerService {

    private final boolean notificateNextDay;
    private final DayOfWeek notificateNextDayOfWeek;

    protected BaseRewardNotificationThresholdBasedHandler(
            @Value("${app.rewards-notification.threshold-notification-day}") String notificationDay,
            RewardsNotificationRepository rewardsNotificationRepository, RewardsNotificationMapper mapper) {
        super(rewardsNotificationRepository, mapper);

        if("TOMORROW".equalsIgnoreCase(notificationDay)){
            notificateNextDay=true;
            notificateNextDayOfWeek=null;
        } else if(notificationDay != null && notificationDay.startsWith("NEXT_")){
            notificateNextDay=false;
            notificateNextDayOfWeek=DayOfWeek.valueOf(notificationDay.substring(5).toUpperCase());
        } else {
            throw new IllegalArgumentException("Invalid notificationDay, allowed TOMORROW or 'NEXT_' followed by the day of week" + notificationDay);
        }
    }

    @Override
    public Mono<RewardsNotification> handle(RewardTransactionDTO trx, RewardNotificationRule rule, Reward reward) {
        String beneficiaryId = getBeneficiaryId(trx, rule);
        return rewardsNotificationRepository.findByBeneficiaryIdAndInitiativeIdAndNotificationDateAndStatusAndOrdinaryIdIsNull(beneficiaryId, rule.getInitiativeId(), null, RewardNotificationStatus.TO_SEND)
                .switchIfEmpty(Mono.defer(()->handleNoOpenNotification(trx, rule, reward)))
                .last()
                .doOnNext(n -> {
                    updateReward(trx, rule, reward, n);

                    n.setNotificationDate(
                            isThresholdReached(rule, n, reward)
                                    ? calculateNotificationDate()
                                    : null);
                });
    }

    protected abstract boolean isThresholdReached(RewardNotificationRule rule, RewardsNotification n, Reward reward);

    public LocalDate calculateNotificationDate() {
        if(notificateNextDay){
            return LocalDate.now().plusDays(1);
        } else {
            return LocalDate.now().with(TemporalAdjusters.next(notificateNextDayOfWeek));
        }
    }

    private Mono<RewardsNotification> handleNoOpenNotification(RewardTransactionDTO trx, RewardNotificationRule rule, Reward reward) {
        Flux<RewardsNotification> findFutureIfRefund;
        if(reward.getAccruedRewardCents() <= 0){
            log.debug("[REWARD_NOTIFICATION] searching for future notification for userId {} and initiativeId {}", trx.getUserId(), rule.getInitiativeId());
            String beneficiaryId = getBeneficiaryId(trx, rule);
            findFutureIfRefund = rewardsNotificationRepository.findByBeneficiaryIdAndInitiativeIdAndNotificationDateGreaterThanAndStatusAndOrdinaryIdIsNull(beneficiaryId, rule.getInitiativeId(), LocalDate.now(), RewardNotificationStatus.TO_SEND);
        } else {
            findFutureIfRefund = Flux.empty();
        }
        return findFutureIfRefund
                .switchIfEmpty(
                        createNewNotificationWithProgressiveId(trx, rule, null, buildNotificationId(trx, rule))
                )
                .last();
    }

    private String buildNotificationId(RewardTransactionDTO trx, RewardNotificationRule rule) {
        return "%s_%s".formatted(
                getBeneficiaryId(trx, rule),
                rule.getInitiativeId()
        );
    }

}
