package it.gov.pagopa.reward.notification.service;

import it.gov.pagopa.reward.notification.dto.rest.UserInfoPDV;
import it.gov.pagopa.reward.notification.model.User;
import it.gov.pagopa.reward.notification.rest.UserRestClient;
import it.gov.pagopa.reward.notification.test.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    private UserRestClient userRestClientMock;

    private UserService userService;

    private final int initialSizeCache = 2;
    private Field userCacheField;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRestClientMock);

        Map<String, User>  userCacheTest = new ConcurrentHashMap<>();
        IntStream.range(0, initialSizeCache).forEach(i -> userCacheTest.put("USERID_%d".formatted(i),
                        User.builder().fiscalCode("FISCALCODE_%d".formatted(i)).build()));

        userCacheField = ReflectionUtils.findField(UserServiceImpl.class, "userCache");
        Assertions.assertNotNull(userCacheField);
        ReflectionUtils.makeAccessible(userCacheField);
        ReflectionUtils.setField(userCacheField, userService,userCacheTest);
    }

    @Test
    void getUserInfoNotInCache(){
        // Given
        String userIdTest = "USERID_NEW";
        Mockito.when(userRestClientMock.retrieveUserInfo(userIdTest)).thenReturn(Mono.just(UserInfoPDV.builder().pii("FISCALCODE_RETRIEVED").build()));

        // When
        Map<String, User> inspectCache = retrieveCache();
        Assertions.assertNull(inspectCache.get(userIdTest));
        Assertions.assertEquals(initialSizeCache,inspectCache.size());

        User result = userService.getUserInfo(userIdTest).block();


        // Then
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result);
        Assertions.assertEquals("FISCALCODE_RETRIEVED", result.getFiscalCode());
        Assertions.assertNotNull(inspectCache.get(userIdTest));
        Assertions.assertEquals(initialSizeCache+1,inspectCache.size());


        Mockito.verify(userRestClientMock).retrieveUserInfo(userIdTest);
    }

    @Test
    void getUserInfoInCache(){
        // Given
        String userIdTest = "USERID_0";

        // When
        Map<String, User> inspectCache = retrieveCache();
        Assertions.assertNotNull(inspectCache.get(userIdTest));
        Assertions.assertEquals(initialSizeCache,inspectCache.size());

        User result = userService.getUserInfo(userIdTest).block();

        // Then
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result);
        Assertions.assertEquals("FISCALCODE_0", result.getFiscalCode());
        Assertions.assertNotNull(inspectCache.get(userIdTest));
        Assertions.assertEquals(initialSizeCache,inspectCache.size());

        Mockito.verify(userRestClientMock, Mockito.never()).retrieveUserInfo(userIdTest);
    }

    private Map<String, User> retrieveCache() {
        Object cacheBefore = ReflectionUtils.getField(userCacheField, userService);
        Assertions.assertNotNull(cacheBefore);
        return (Map<String, User>) cacheBefore;
    }
}