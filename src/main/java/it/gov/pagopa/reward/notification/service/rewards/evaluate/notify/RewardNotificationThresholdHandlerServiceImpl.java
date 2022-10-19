package it.gov.pagopa.reward.notification.service.rewards.evaluate.notify;

import it.gov.pagopa.reward.notification.dto.mapper.RewardsNotificationMapper;
import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
public class RewardNotificationThresholdHandlerServiceImpl extends BaseRewardNotificationHandlerService implements RewardNotificationHandlerService {

    public RewardNotificationThresholdHandlerServiceImpl(RewardsNotificationRepository rewardsNotificationRepository, RewardsNotificationMapper mapper) {
        super(rewardsNotificationRepository, mapper);
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
                                    ? LocalDate.now().plusDays(1)
                                    : null);
                });
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
