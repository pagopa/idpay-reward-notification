package it.gov.pagopa.reward.notification.service.commands.ops;

import reactor.core.publisher.Mono;

public interface DeleteInitiativeService {
    Mono<String> execute(String initiativeId);
}
