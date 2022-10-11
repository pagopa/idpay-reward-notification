package it.gov.pagopa.reward.notification.event.consumer;

import it.gov.pagopa.reward.notification.service.rewards.RewardsMediatorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Configuration
public class RewardResponseConsumerConfig {

    private final RewardsMediatorService rewardsMediatorService;

    public RewardResponseConsumerConfig(RewardsMediatorService rewardsMediatorService) {
        this.rewardsMediatorService = rewardsMediatorService;
    }

    @Bean
    public Consumer<Flux<Message<String>>> rewardTrxConsumer(){
        return rewardsMediatorService::execute;
    }
}
