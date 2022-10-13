package it.gov.pagopa.reward.notification.service.iban.request;

import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

/**
 * This component given an iban will:
 * <ol>
 *     <li>map into model</li>
 *     <li>store it inside DB</li>
 * </ol>
 * */
public interface IbanRequestMediatorService {
    void execute(Flux<Message<String>> ibanRequestDTOFlux);
}