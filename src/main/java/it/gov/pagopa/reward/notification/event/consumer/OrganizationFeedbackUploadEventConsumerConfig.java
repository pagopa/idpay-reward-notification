package it.gov.pagopa.reward.notification.event.consumer;

import it.gov.pagopa.reward.notification.service.feedback.RewardNotificationFeedbackMediatorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Configuration
public class OrganizationFeedbackUploadEventConsumerConfig {
    private final RewardNotificationFeedbackMediatorService feedbackMediatorService;

    public OrganizationFeedbackUploadEventConsumerConfig(RewardNotificationFeedbackMediatorService feedbackMediatorService) {
        this.feedbackMediatorService = feedbackMediatorService;
    }

    @Bean
    public Consumer<Flux<Message<String>>> rewardNotificationUploadConsumer(){
        return feedbackMediatorService::execute;
    }
}
