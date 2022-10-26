package it.gov.pagopa.reward.notification.rest;

import it.gov.pagopa.reward.notification.dto.rest.UserInfoPDV;
import reactor.core.publisher.Mono;

public interface UserRestClient {
    Mono<UserInfoPDV> retrieveUserInfo(String userId);
}
