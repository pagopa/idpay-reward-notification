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
    private final Map<String, User> userCache = new ConcurrentHashMap<>();
    private final UserRestClient userRestClient;

    public UserServiceImpl(UserRestClient userRestClient) {
        this.userRestClient = userRestClient;
    }

    @Override
    public Mono<User> getUserInfo(String userId) {
        User userFromCache = userCache.get(userId);
        if(userFromCache != null){
            return Mono.just(userFromCache);
        }else {
            return userRestClient.retrieveUserInfo(userId)
                    .map(s -> User.builder().fiscalCode(s.getPii()).build())
                    .doOnNext(u -> {
                        userCache.put(userId,u);
                        log.info("Added into map user info with userId: {}", userId);
                    });
        }
    }
}
