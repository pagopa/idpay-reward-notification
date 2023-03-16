package it.gov.pagopa.reward.notification.connector.email;

import it.gov.pagopa.reward.notification.dto.email.EmailMessageDTO;
import reactor.core.publisher.Mono;

public interface EmailNotificationRestClient {

    Mono<Void> send(EmailMessageDTO emailMessage);
}
