package it.gov.pagopa.reward.notification.service;

import it.gov.pagopa.reward.notification.model.User;
import reactor.core.publisher.Mono;

public interface UserService {
    User getUserFromCache(String key);
    void putUserToCache(String key, User value);
    Mono<User> getUserInfo(String userId);
}
