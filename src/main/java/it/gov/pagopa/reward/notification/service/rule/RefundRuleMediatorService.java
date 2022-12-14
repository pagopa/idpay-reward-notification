package it.gov.pagopa.reward.notification.service.rule;

import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

/**
 * This component given an initiative will:
 * <ol>
 *     <li>map into model</li>
 *     <li>store it inside DB</li>
 * </ol>
 * */
public interface RefundRuleMediatorService {
    void execute(Flux<Message<String>> initiativeDTOFlux);
}
