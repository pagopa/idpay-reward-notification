package it.gov.pagopa.reward.notification.service;

import it.gov.pagopa.reward.notification.model.User;
import it.gov.pagopa.reward.notification.rest.UserRestClient;
import it.gov.pagopa.reward.notification.test.utils.TestUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.util.stream.IntStream;

class UserServiceImplTest {

    @Test
    void getUserInfo(){
        // Given
        UserRestClient userRestClientMock = Mockito.mock(UserRestClient.class);
        UserService userService = new UserServiceImpl(userRestClientMock);

        //region initializing user map
        int initialSize = 2;
        IntStream.range(0,initialSize).mapToObj(i -> Pair.of(i,User.builder()
                .cf("FISCALCODE_%d".formatted(i)).build()))
                .forEach(p -> userService.putUserToCache("USERID_%d".formatted(p.getLeft()), p.getRight()));
        //endregion

        String userIdTest = "USERID_%d".formatted(initialSize);
        Mockito.when(userRestClientMock.retrieveUserInfo(userIdTest)).thenReturn(Mono.just("FISCALCODE_RETRIEVED"));

        // When
        User retrieveFromCacheBefore = userService.getUserFromCache(userIdTest);
        User result = userService.getUserInfo(userIdTest).block();
        User retrieveFromCacheAfter = userService.getUserFromCache(userIdTest);

        // Then
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result);
        Assertions.assertEquals("FISCALCODE_RETRIEVED", result.getCf());
        Assertions.assertNull(retrieveFromCacheBefore);
        Assertions.assertEquals(result,retrieveFromCacheAfter);

        Mockito.verify(userRestClientMock).retrieveUserInfo(userIdTest);
    }

}