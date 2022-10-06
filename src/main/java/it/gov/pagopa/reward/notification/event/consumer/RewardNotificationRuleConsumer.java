package it.gov.pagopa.reward.notification.event.consumer;

import it.gov.pagopa.reward.notification.service.RewardNotificationRuleMediator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
public class RewardNotificationRuleConsumer {

    private final RewardNotificationRuleMediator rewardNotificationRuleMediator;

    public RewardNotificationRuleConsumer(RewardNotificationRuleMediator rewardNotificationRuleMediator) {
        this.rewardNotificationRuleMediator = rewardNotificationRuleMediator;
    }

    @Bean
    public Consumer<Flux<Message<String>>> rewardNotificationConsumer(){
        return rewardNotificationRuleMediator::execute;
    }
}
