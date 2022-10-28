package it.gov.pagopa.reward.notification.rest;

import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.dto.rest.UserInfoPDV;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.Exceptions;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.reward.notification.rest.UserRestClientImpl=WARN",
})
class UserRestClientImplTest extends BaseIntegrationTest {

    @Autowired
    private UserRestClient userRestClient;


    @Test
    void retrieveUserInfoOk() {
        String userId = "USERID_OK_1";

        UserInfoPDV result = userRestClient.retrieveUserInfo(userId).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals("fiscalCode",result.getPii());
    }

    @Test
    void retrieveUserInfoNotFound() {
        String userId = "USERID_NOTFOUND_1";

        try{
            userRestClient.retrieveUserInfo(userId).block();
        }catch (Throwable e){
            Assertions.assertTrue(e instanceof HttpClientErrorException);
            Assertions.assertEquals(HttpClientErrorException.NotFound.class, e.getClass());
            Assertions.assertEquals("An error occurred when call PDV with userId %s: %s".formatted(userId, HttpStatus.NOT_FOUND.name()), e.getMessage());
        }
    }

    @Test
    void retrieveUserInfoInternalServerError() {
        String userId = "USERID_INTERNALSERVERERROR_1";

        try{
            userRestClient.retrieveUserInfo(userId).block();
        }catch (Throwable e){
            Assertions.assertTrue(e instanceof HttpClientErrorException);
            Assertions.assertEquals(HttpClientErrorException.class, e.getClass());
            Assertions.assertEquals("An error occurred when call PDV with userId %s: %s".formatted(userId, HttpStatus.INTERNAL_SERVER_ERROR.name()), e.getMessage());
        }
    }

    @Test
    void retrieveUserInfoBadRequest() {
        String userId = "USERID_BADREQUEST_1";

        try{
            userRestClient.retrieveUserInfo(userId).block();
        }catch (Throwable e){
            Assertions.assertTrue(e instanceof HttpClientErrorException);
            Assertions.assertEquals(HttpClientErrorException.BadRequest.class, e.getClass());
            Assertions.assertEquals("An error occurred when call PDV with userId %s: %s".formatted(userId, HttpStatus.BAD_REQUEST.name()), e.getMessage());
        }
    }

    @Test
    void retrieveUserInfoTooManyRequest() {
        String userId = "USERID_TOOMANYREQUEST_1";

        try{
            userRestClient.retrieveUserInfo(userId).block();
        }catch (Throwable e){
            Assertions.assertTrue(Exceptions.isRetryExhausted(e));
        }
    }

    @Test
    void retrieveUserInfoHttpNotHandler() {
        String userId = "USERID_HTTPNOTHANDLER_FORBIDEN_1";

        try{
            userRestClient.retrieveUserInfo(userId).block();
        }catch (Throwable e){
            e.printStackTrace();
            Assertions.assertTrue(e instanceof WebClientException);
        }
    }

}