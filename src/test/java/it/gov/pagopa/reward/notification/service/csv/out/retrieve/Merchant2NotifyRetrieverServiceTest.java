package it.gov.pagopa.reward.notification.service.csv.out.retrieve;

import it.gov.pagopa.reward.notification.connector.merchant.MerchantRestClient;
import it.gov.pagopa.reward.notification.dto.merchant.MerchantDetailDTO;
import it.gov.pagopa.reward.notification.enums.BeneficiaryType;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.csv.RewardNotificationNotifierService;
import it.gov.pagopa.reward.notification.utils.ExportCsvConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class Merchant2NotifyRetrieverServiceTest {

    public static final String INITIATIVEID = "INITIATIVEID";
    public static final String MERCHANTID = "MERCHANTID";
    @Mock private MerchantRestClient merchantRestClientMock;
    @Mock private RewardsNotificationRepository rewardsNotificationRepositoryMock;
    @Mock private RewardNotificationNotifierService errorNotifierServiceMock;

    private Merchant2NotifyRetrieverService service;

    @BeforeEach
    void init() {
        service = new Merchant2NotifyRetrieverServiceImpl(merchantRestClientMock, rewardsNotificationRepositoryMock, errorNotifierServiceMock);
    }

    @Test
    void noMerchantTest(){
        // Given
        RewardsNotification reward = buildRewardsNotification();

        Mockito.when(merchantRestClientMock.getMerchant(INITIATIVEID, MERCHANTID)).thenReturn(Mono.empty());
        Mockito.when(rewardsNotificationRepositoryMock.save(Mockito.same(reward))).thenReturn(Mono.just(reward));
        Mockito.when(errorNotifierServiceMock.notify(Mockito.same(reward), Mockito.eq(0L))).thenReturn(Mono.just(reward));

        // When
        Pair<RewardsNotification, MerchantDetailDTO> result = service.retrieve(reward).block();

        // Then
        Assertions.assertNull(result);

        Assertions.assertEquals(RewardNotificationStatus.ERROR, reward.getStatus());
        Assertions.assertEquals(ExportCsvConstants.EXPORT_REJECTION_REASON_CF_NOT_FOUND, reward.getRejectionReason());
        Assertions.assertEquals(ExportCsvConstants.EXPORT_REJECTION_REASON_CF_NOT_FOUND, reward.getResultCode());
        Assertions.assertNotNull(reward.getExportDate());

        Mockito.verifyNoMoreInteractions(merchantRestClientMock, rewardsNotificationRepositoryMock, errorNotifierServiceMock);
    }

    @Test
    void successfulTest(){
        // Given
        RewardsNotification reward = buildRewardsNotification();

        MerchantDetailDTO expectedMerchantDetail = MerchantDetailDTO.builder()
                .fiscalCode("CF")
                .iban("IBAN")
                .businessName("MERCHANT")
                .build();
        Mockito.when(merchantRestClientMock.getMerchant(INITIATIVEID, MERCHANTID)).thenReturn(Mono.just(expectedMerchantDetail));

        // When
        Pair<RewardsNotification, MerchantDetailDTO> result = service.retrieve(reward).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertSame(reward, result.getKey());
        Assertions.assertSame(expectedMerchantDetail, result.getValue());

        Assertions.assertEquals(RewardNotificationStatus.TO_SEND, result.getKey().getStatus());
        Assertions.assertNull(result.getKey().getRejectionReason());
        Assertions.assertNull(result.getKey().getResultCode());

        Mockito.verifyNoMoreInteractions(merchantRestClientMock, rewardsNotificationRepositoryMock, errorNotifierServiceMock);
    }

    @Test
    void testWhenException(){
        // Given
        RewardsNotification reward = buildRewardsNotification();

        Mockito.when(merchantRestClientMock.getMerchant(INITIATIVEID, MERCHANTID)).thenReturn(Mono.error(new RuntimeException("DUMMY")));

        // When
        Pair<RewardsNotification, MerchantDetailDTO> result = service.retrieve(reward).block();

        // Then
        Assertions.assertNull(result);

        Mockito.verifyNoMoreInteractions(merchantRestClientMock, rewardsNotificationRepositoryMock, errorNotifierServiceMock);
    }




    private static RewardsNotification buildRewardsNotification() {
        RewardsNotification reward = new RewardsNotification();
        reward.setInitiativeId(INITIATIVEID);
        reward.setBeneficiaryId(MERCHANTID);
        reward.setBeneficiaryType(BeneficiaryType.MERCHANT);
        reward.setStatus(RewardNotificationStatus.TO_SEND);
        return reward;
    }
}