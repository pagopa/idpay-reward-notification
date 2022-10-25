package it.gov.pagopa.reward.notification.service;

import it.gov.pagopa.reward.notification.model.User;
import it.gov.pagopa.reward.notification.rest.UserRestClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class UserServiceImpl implements UserService{
    private static final Map<String, User> USER_CACHE = new ConcurrentHashMap<>();
    private final UserRestClient userRestClient;

    public UserServiceImpl(UserRestClient userRestClient) {
        this.userRestClient = userRestClient;
    }

    @Override
    public User getUserFromCache(String key) {
        return USER_CACHE.get(key);
    }

    @Override
    public void putUserToCache(String key, User value) {
        USER_CACHE.put(key, value);
    }

    @Override
    public Mono<User> getUserInfo(String userId) {
        User userFromCache = getUserFromCache(userId);
        if(userFromCache != null){
            return Mono.just(userFromCache);
        }else {
            return Mono.just(userId)
                    .flatMap(userRestClient::retrieveUserInfo)
                    .map(s -> User.builder().fiscalCode(s.getPii()).build())
                    .doOnNext(u -> {
                        putUserToCache(userId,u);
                        log.info("Added into map user info with userId: {}", userId);
                    });
        }
    }
}
