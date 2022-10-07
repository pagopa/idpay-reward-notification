package it.gov.pagopa.reward.notification.event.consumer;

import it.gov.pagopa.reward.notification.service.RefundRuleMediator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
public class RefundRuleConsumerConfig {

    private final RefundRuleMediator refundRuleMediator;

    public RefundRuleConsumerConfig(RefundRuleMediator refundRuleMediator) {
        this.refundRuleMediator = refundRuleMediator;
    }

    @Bean
    public Consumer<Flux<Message<String>>> refundRuleConsumer(){
        return refundRuleMediator::execute;
    }
}
