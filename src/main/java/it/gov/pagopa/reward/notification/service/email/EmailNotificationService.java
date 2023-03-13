package it.gov.pagopa.reward.notification.service.email;

import reactor.core.publisher.Mono;

public interface EmailNotificationService {

    Mono<Void> notifyOrganization();
}
