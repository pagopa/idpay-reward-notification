package it.gov.pagopa.reward.notification.connector.wallet;

import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.exception.ClientExceptionNoBody;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;


@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.reward.notification.rest.WalletRestClientImpl=WARN",
})
class WalletRestClientImplTest extends BaseIntegrationTest {

    @Autowired
    private WalletRestClient walletRestClient;

    @Test
    void testOk() {
        String userId = "USERID_OK_1";

        ResponseEntity<Void> result = walletRestClient.suspend("INITIATIVEID", userId).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
    }


    @Test
    void testKo() {
        String userId = "USERID_KO_1";

        Executable executable = () -> walletRestClient.suspend("INITIATIVEID", userId).block();

        Assertions.assertThrows(ClientExceptionNoBody.class, executable);
    }
}