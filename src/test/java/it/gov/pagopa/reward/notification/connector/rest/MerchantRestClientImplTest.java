package it.gov.pagopa.reward.notification.connector.rest;

import it.gov.pagopa.reward.notification.BaseIntegrationTestDeprecated;
import it.gov.pagopa.reward.notification.dto.rest.MerchantDetailDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import reactor.core.Exceptions;

import java.time.LocalDateTime;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.reward.notification.rest.MerchantRestClientImpl=WARN",
        "app.merchant.retry.delay-millis=100",
        "app.merchant.retry.max-attempts=1"
})
class MerchantRestClientImplTest extends BaseIntegrationTestDeprecated {

    @Autowired
    private MerchantRestClient merchantRestClient;

    @Test
    void testOk() {
        String merchantId = "MERCHANTID_OK_1";

        MerchantDetailDTO result = merchantRestClient.getMerchant(merchantId, "ORGANIZATIONID", "INITIATIVEID").block();

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
    void testKo() {
        String merchantId = "MERCHANTID_NOTFOUND_1";

        MerchantDetailDTO result = merchantRestClient.getMerchant(merchantId, "ORGANIZATIONID", "INITIATIVEID").block();

        Assertions.assertNull(result);
    }

    @Test
    void retrieveUserInfoTooManyRequest() {
        String merchantId = "MERCHANTID_TOOMANYREQUESTS_1";

        try{
            merchantRestClient.getMerchant(merchantId, "ORGANIZATIONID", "INITIATIVEID").block();
        }catch (Throwable e){
            Assertions.assertTrue(Exceptions.isRetryExhausted(e));
        }
    }
}