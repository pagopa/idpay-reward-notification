package it.gov.pagopa.reward.notification.service.iban.outcome;

import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

/**
 * This component given an iban will:
 * <ol>
 *     <li>map into model</li>
 *     <li>delete it inside DB</li>
 * </ol>
 * */
public interface IbanOutcomeMediatorService {
    void execute(Flux<Message<String>> ibanOutcomeDTOFlux);
}