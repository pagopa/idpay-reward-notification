package it.gov.pagopa.reward.notification.service.csv;

import it.gov.pagopa.reward.notification.model.RewardsNotification;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class RewardNotificationErrorNotifierServiceImpl implements RewardNotificationErrorNotifierService {
    @Override
    public Mono<RewardsNotification> notify(RewardsNotification reward) {
        // TODO notification towards users?
        return Mono.just(reward);
    }
}
