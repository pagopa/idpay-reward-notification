package it.gov.pagopa.reward.notification.event.consumer;

import it.gov.pagopa.reward.notification.service.rule.RefundRuleMediatorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
public class RefundRuleConsumerConfig {

    private final RefundRuleMediatorService refundRuleMediatorService;

    public RefundRuleConsumerConfig(RefundRuleMediatorService refundRuleMediatorService) {
        this.refundRuleMediatorService = refundRuleMediatorService;
    }

    @Bean
    public Consumer<Flux<Message<String>>> refundRuleConsumer(){
        return refundRuleMediatorService::execute;
    }
}
