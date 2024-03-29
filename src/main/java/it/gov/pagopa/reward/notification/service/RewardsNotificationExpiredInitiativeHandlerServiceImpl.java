package it.gov.pagopa.reward.notification.service;

import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardNotificationRuleRepository;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@Service
@Slf4j
public class RewardsNotificationExpiredInitiativeHandlerServiceImpl implements RewardsNotificationExpiredInitiativeHandlerService {

    private final int dayBefore;

    private final RewardNotificationRuleRepository rewardNotificationRuleRepository;
    private final RewardsNotificationRepository rewardsNotificationRepository;

    public RewardsNotificationExpiredInitiativeHandlerServiceImpl(RewardNotificationRuleRepository rewardNotificationRuleRepository, RewardsNotificationRepository rewardsNotificationRepository, @Value("${app.rewards-notification.expired-initiatives.day-before}") int dayBefore) {
        this.rewardNotificationRuleRepository = rewardNotificationRuleRepository;
        this.rewardsNotificationRepository = rewardsNotificationRepository;
        this.dayBefore = dayBefore;
    }

    @Scheduled(cron = "${app.rewards-notification.expired-initiatives.schedule}")
    void schedule() {
        log.debug("[REWARDS_NOTIFICATION_EXPIRED_INITIATIVE][SCHEDULE] Starting schedule to update notificationDate");
        this.handle()
                .subscribe(x -> log.debug("[REWARDS_NOTIFICATION_EXPIRED_INITIATIVE][SCHEDULE] Completed schedule to update notificationDate"));
    }

    @Override
    public Flux<RewardsNotification> handle() {
        LocalDate today = LocalDate.now();

        return rewardNotificationRuleRepository
                .findByAccumulatedAmountNotNullAndEndDateBetween(
                        today.minusDays(dayBefore),
                        today
                )
                .flatMap(rule -> rewardsNotificationRepository
                        .findByInitiativeIdAndNotificationDate(
                                rule.getInitiativeId(),
                                null
                        )
                )
                .doOnNext(n -> n.setNotificationDate(today.plusDays(1)))
                .flatMap(rewardsNotificationRepository::save);
    }

}
