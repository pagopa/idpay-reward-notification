package it.gov.pagopa.reward.notification.service.rewards.evaluate.notify;

import it.gov.pagopa.reward.notification.dto.mapper.RewardsNotificationMapper;
import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Service
@Slf4j
public class RewardsNotificationExpiredInitiativeHandlerService extends BaseRewardNotificationHandlerService {
    LocalDate tmp = LocalDate.now();  // TODO use days set in ENV to search expired initiatives

    protected RewardsNotificationExpiredInitiativeHandlerService(RewardsNotificationRepository rewardsNotificationRepository, RewardsNotificationMapper mapper) {
        super(rewardsNotificationRepository, mapper);
    }

    @Scheduled(cron = "${app.rewards-notification.expired-initiatives.schedule}")
    void schedule(RewardTransactionDTO trx, RewardNotificationRule rule, Reward reward) {
        log.debug("[REWARDS_NOTIFICATION_EXPIRED_INITIATIVE][SCHEDULE] Starting schedule to update notificationDate");
        this.handle(trx, rule, reward)
                .subscribe(x -> log.debug("[REWARDS_NOTIFICATION_EXPIRED_INITIATIVE][SCHEDULE] Completed schedule to update notificationDate"));
    }

    @Override
    public Mono<RewardsNotification> handle(RewardTransactionDTO trx, RewardNotificationRule rule, Reward reward) {
        // TODO search initiatives with accumulatedAmount != null & endDate before today - ${since-days}
        return rewardsNotificationRepository.findByUserIdAndInitiativeIdAndNotificationDate(trx.getUserId(), rule.getInitiativeId(), null)
                .last()
                .doOnNext(n -> n.setNotificationDate(LocalDate.now().plusDays(1)));
    }


}
