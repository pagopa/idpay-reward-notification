package it.gov.pagopa.reward.notification.service.feedback;

import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

public interface RewardNotificationFeedbackMediatorService {
    void execute(Flux<Message<String>> rewardNotificationStorageEventsFlux);
}
