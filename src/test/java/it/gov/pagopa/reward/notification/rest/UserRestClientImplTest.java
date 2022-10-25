package it.gov.pagopa.reward.notification.rest;

import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.dto.rest.UserInfoPDV;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Mono;

class UserRestClientImplTest extends BaseIntegrationTest {

    @Autowired
    private UserRestClient userRestClient;

    @Test
    void retrieveUserInfoOk() {
        // Given
        String userId = "USERID_OK_1";

        // When
        UserInfoPDV result = userRestClient.retrieveUserInfo(userId).block();

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals("fiscalCode",result.getPii());
    }

    @Test
    void retrieveUserInfoNotFound() {
        // Given
        String userId = "USERID_NOTFOUND_1";

        // When
        Mono<UserInfoPDV> result = userRestClient.retrieveUserInfo(userId);

        try{
            userRestClient.retrieveUserInfo(userId).block();
        }catch (Throwable e){
            Assertions.assertEquals(HttpClientErrorException.class, e.getClass());
            Assertions.assertEquals("An error occurred when call PDV with userId %s: %s".formatted(userId, HttpStatus.NOT_FOUND.name()), e.getMessage());
        }
    }

    @Test
    void retrieveUserInfoInternalServerError() {
        // Given
        String userId = "USERID_INTERNALSERVERERROR_1";

        try{
            userRestClient.retrieveUserInfo(userId).block();
        }catch (Throwable e){
            Assertions.assertEquals(HttpClientErrorException.class, e.getClass());
            Assertions.assertEquals("An error occurred when call PDV with userId %s: %s".formatted(userId, HttpStatus.INTERNAL_SERVER_ERROR.name()), e.getMessage());
        }
    }
}