package it.gov.pagopa.reward.notification.event.consumer;

import it.gov.pagopa.reward.notification.service.iban.request.IbanRequestMediatorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Configuration
public class IbanRequestConsumerConfig {
    private final IbanRequestMediatorService ibanRequestMediatorService;

    public IbanRequestConsumerConfig(IbanRequestMediatorService ibanRequestMediatorService) {
        this.ibanRequestMediatorService = ibanRequestMediatorService;
    }

    @Bean
    public Consumer<Flux<Message<String>>> ibanRequestConsumer(){
        return ibanRequestMediatorService::execute;
    }
}
