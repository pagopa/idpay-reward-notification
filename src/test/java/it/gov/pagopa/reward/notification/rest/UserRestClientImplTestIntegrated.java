package it.gov.pagopa.reward.notification.rest;

import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.dto.rest.UserInfoPDV;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@SuppressWarnings("squid:S3577") // suppressing class name not match alert
@ContextConfiguration(initializers = UserRestClientImplTestIntegrated.AddPDVProperties.class)
class UserRestClientImplTestIntegrated extends BaseIntegrationTest {

    @Autowired
    private UserRestClient userRestClient;

    @Value("${app.pdv.userIdOk}")
    private String userIdOK;
    @Value("${app.pdv.userFiscalCodeExpected}")
    private String fiscalCodeOKExpected;
    @Value("${app.pdv.userIdNotFound}")
    private String userIdNotFound;

    @Test
    void retrieveUserInfoOk() {
        UserInfoPDV result = userRestClient.retrieveUserInfo(userIdOK).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(fiscalCodeOKExpected,result.getPii());

    }

    @Test
    void retrieveUserInfoNotFound() {
        try{
            userRestClient.retrieveUserInfo(userIdNotFound).block();
        }catch (Throwable e){
            Assertions.assertTrue(e instanceof WebClientException);
            Assertions.assertEquals(WebClientResponseException.NotFound.class,e.getClass());
        }
    }

    public static class AddPDVProperties implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @SneakyThrows
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertySourceUtils.addPropertiesFilesToEnvironment(applicationContext,"classpath:/secrets/appPdv.properties");
        }
    }
}