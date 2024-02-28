package it.gov.pagopa.reward.notification.connector.rest;

import it.gov.pagopa.common.reactive.rest.config.WebClientConfig;
import it.gov.pagopa.reward.notification.BaseWireMockTest;
import it.gov.pagopa.reward.notification.dto.rest.MerchantDetailDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import reactor.core.Exceptions;

import java.time.LocalDateTime;

import static it.gov.pagopa.reward.notification.BaseWireMockTest.WIREMOCK_TEST_PROP2BASEPATH_MAP_PREFIX;

@ContextConfiguration(
        classes = {
                MerchantRestClientImpl.class,
                WebClientConfig.class
        })
@TestPropertySource(properties = {
        WIREMOCK_TEST_PROP2BASEPATH_MAP_PREFIX + "app.merchant.base-url="
})
class MerchantRestClientImplTest extends BaseWireMockTest {

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