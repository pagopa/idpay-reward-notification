package it.gov.pagopa.reward.notification.event.consumer;

import it.gov.pagopa.reward.notification.service.iban.outcome.IbanOutcomeMediatorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Configuration
public class IbanOutcomeConsumerConfig {

    private final IbanOutcomeMediatorService ibanOutcomeMediatorService;

    public IbanOutcomeConsumerConfig(IbanOutcomeMediatorService ibanOutcomeMediatorService) {
        this.ibanOutcomeMediatorService = ibanOutcomeMediatorService;
    }

    @Bean
    public Consumer<Flux<Message<String>>> ibanOutcomeConsumer(){
        return ibanOutcomeMediatorService::execute;
    }
}