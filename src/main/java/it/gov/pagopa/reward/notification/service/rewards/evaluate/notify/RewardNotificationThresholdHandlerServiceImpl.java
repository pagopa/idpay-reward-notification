package it.gov.pagopa.reward.notification.service.rewards.evaluate.notify;

import it.gov.pagopa.reward.notification.dto.mapper.RewardsNotificationMapper;
import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Slf4j
@Service
public class RewardNotificationThresholdHandlerServiceImpl extends BaseRewardNotificationHandlerService implements RewardNotificationHandlerService {

    private final boolean notificateNextDay;
    private final DayOfWeek notificateNextDayOfWeek;

    public RewardNotificationThresholdHandlerServiceImpl(
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
        return rewardsNotificationRepository.findByUserIdAndInitiativeIdAndNotificationDate(trx.getUserId(), rule.getInitiativeId(), null)
                .switchIfEmpty(handleNoOpenNotification(trx, rule, reward))
                .last()
                .doOnNext(n -> {
                    updateReward(trx, rule, reward, n);

                    n.setNotificationDate(
                            n.getRewardCents() >= rule.getAccumulatedAmount().getRefundThresholdCents()
                                    ? calculateNotificationDate()
                                    : null);
                });
    }

    private LocalDate calculateNotificationDate() {
        if(notificateNextDay){
            return LocalDate.now().plusDays(1);
        } else {
            return LocalDate.now().with(TemporalAdjusters.next(notificateNextDayOfWeek));
        }
    }

    private Mono<RewardsNotification> handleNoOpenNotification(RewardTransactionDTO trx, RewardNotificationRule rule, Reward reward) {
        Flux<RewardsNotification> findFutureIfRefund;
        if(reward.getAccruedReward().compareTo(BigDecimal.ZERO) < 0){
            log.debug("[REWARD_NOTIFICATION] searching for future notification for userId {} and initiativeId {}", trx.getUserId(), rule.getInitiativeId());
            findFutureIfRefund = rewardsNotificationRepository.findByUserIdAndInitiativeIdAndNotificationDateGreaterThan(trx.getUserId(), rule.getInitiativeId(), LocalDate.now());
        } else {
            findFutureIfRefund = Flux.empty();
        }
        return findFutureIfRefund
                .switchIfEmpty(
                    createNewNotification(trx, rule, null, buildNotificationId(trx, rule))
                    .doOnNext(n -> n.setId("%s_%d".formatted(n.getId(), n.getProgressive())))
                )
                .last();
    }

    private String buildNotificationId(RewardTransactionDTO trx, RewardNotificationRule rule) {
        return "%s_%s".formatted(
                trx.getUserId(),
                rule.getInitiativeId()
        );
    }

}
