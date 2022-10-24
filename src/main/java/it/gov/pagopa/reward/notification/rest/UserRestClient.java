package it.gov.pagopa.reward.notification.rest;

import reactor.core.publisher.Mono;

public interface UserRestClient {
    Mono<String> retrieveUserInfo(String token);
}
