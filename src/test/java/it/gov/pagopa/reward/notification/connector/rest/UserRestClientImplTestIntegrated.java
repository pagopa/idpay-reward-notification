package it.gov.pagopa.reward.notification.connector.rest;

import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.dto.rest.UserInfoPDV;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * See confluence page: <a href="https://pagopa.atlassian.net/wiki/spaces/IDPAY/pages/615974424/Secrets+UnitTests">Secrets for UnitTests</a>
 */
@SuppressWarnings({"squid:S3577", "NewClassNamingConvention"}) // suppressing class name not match alert: we are not using the Test suffix in order to let not execute this test by default maven configuration because it depends on properties not pushable. See
@TestPropertySource(locations = {
        "classpath:/secrets/appPdv.properties",
},
        properties = {
                "app.pdv.base-url=https://api.uat.tokenizer.pdv.pagopa.it/tokenizer/v1"
        })
class UserRestClientImplTestIntegrated extends BaseIntegrationTest {

    @Autowired
    private UserRestClient userRestClient;

    @Value("${app.pdv.userIdOk:a85268f9-1d62-4123-8f86-8cf630b60998}")
    private String userIdOK;
    @Value("${app.pdv.userFiscalCodeExpected:A4p9Y4QUlTtutHT}")
    private String fiscalCodeOKExpected;
    @Value("${app.pdv.userIdNotFound:02105b50-9a81-4cd2-8e17-6573ebb09195}")
    private String userIdNotFound;

    @Test
    void retrieveUserInfoOk() {
        UserInfoPDV result = userRestClient.retrieveUserInfo(userIdOK).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(fiscalCodeOKExpected, result.getPii());

    }

    @Test
    void retrieveUserInfoNotFound() {
        try {
            userRestClient.retrieveUserInfo(userIdNotFound).block();
        } catch (WebClientException e) {
            Assertions.assertEquals(WebClientResponseException.NotFound.class, e.getClass());
        }
    }
}