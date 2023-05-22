package it.gov.pagopa.reward.notification.connector.merchant;

import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.dto.merchant.MerchantDetailDTO;
import it.gov.pagopa.reward.notification.exception.ClientExceptionNoBody;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.reward.notification.rest.MerchantRestClientImpl=WARN",
})
class MerchantRestClientImplTest extends BaseIntegrationTest {

    @Autowired
    private MerchantRestClient merchantRestClient;

    @Test
    void testOk() {
        String merchantId = "MERCHANTID_OK_1";

        MerchantDetailDTO result = merchantRestClient.getMerchant("INITIATIVEID", merchantId).block();

        Assertions.assertNotNull(result);

        MerchantDetailDTO expectedMerchant = MerchantDetailDTO.builder()
                .initiativeId("INITIATIVEID")
                .businessName("MERCHANT")
                .legalOfficeAddress("LOA")
                .legalOfficeMunicipality("LOM")
                .legalOfficeProvince("LOP")
                .legalOfficeZipCode("LOZ")
                .certifiedEmail("certified@email.com")
                .fiscalCode("CF")
                .vatNumber("VAT")
                .status("STATUS")
                .iban("IBAN")
                .creationDate(LocalDateTime.of(1970,1,1,0,0))
                .updateDate(LocalDateTime.of(1970,1,1,0,0))
                .build();
        Assertions.assertEquals(expectedMerchant, result);
    }


    @Test
    void testSuspendKo() {
        String merchantId = "MERCHANTID_KO_1";

        Executable executable = () -> merchantRestClient.getMerchant("INITIATIVEID", merchantId).block();

        Assertions.assertThrows(ClientExceptionNoBody.class, executable);
    }
}