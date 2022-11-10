package it.gov.pagopa.reward.notification.service;

import it.gov.pagopa.reward.notification.model.User;
import reactor.core.publisher.Mono;

public interface UserService {
    Mono<User> getUserInfo(String userId);
}
