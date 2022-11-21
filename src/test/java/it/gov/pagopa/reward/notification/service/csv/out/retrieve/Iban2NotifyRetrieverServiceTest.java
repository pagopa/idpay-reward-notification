package it.gov.pagopa.reward.notification.service.csv.out.retrieve;

import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardIban;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardIbanRepository;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.csv.RewardNotificationErrorNotifierService;
import it.gov.pagopa.reward.notification.utils.ExportCsvConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class Iban2NotifyRetrieverServiceTest {

    @Mock private RewardIbanRepository ibanRepositoryMock;
    @Mock private RewardsNotificationRepository rewardsNotificationRepositoryMock;
    @Mock private RewardNotificationErrorNotifierService errorNotifierServiceMock;

    private Iban2NotifyRetrieverService service;

    @BeforeEach
    void init(){
        service = new Iban2NotifyRetrieverServiceImpl(ibanRepositoryMock, rewardsNotificationRepositoryMock, errorNotifierServiceMock);
    }

    @Test
    void noIbanTest(){
        // Given
        RewardsNotification reward = new RewardsNotification();
        reward.setUserId("USERID");
        reward.setInitiativeId("INITIATIATIVEID");
        reward.setStatus(RewardNotificationStatus.TO_SEND);

        Mockito.when(ibanRepositoryMock.findById("USERIDINITIATIATIVEID")).thenReturn(Mono.empty());
        Mockito.when(rewardsNotificationRepositoryMock.save(Mockito.same(reward))).thenReturn(Mono.just(reward));
        Mockito.when(errorNotifierServiceMock.notify(Mockito.same(reward))).thenReturn(Mono.just(reward));

        // When
        RewardsNotification result = service.retrieveIban(reward).block();

        // Then
        Assertions.assertNull(result);

        Assertions.assertEquals(RewardNotificationStatus.ERROR, reward.getStatus());
        Assertions.assertEquals(ExportCsvConstants.EXPORT_REJECTION_REASON_IBAN_NOT_FOUND, reward.getRejectionReason());
        Assertions.assertEquals(ExportCsvConstants.EXPORT_REJECTION_REASON_IBAN_NOT_FOUND, reward.getResultCode());
        Assertions.assertNotNull(reward.getExportDate());

        Mockito.verifyNoMoreInteractions(ibanRepositoryMock, rewardsNotificationRepositoryMock,errorNotifierServiceMock);
    }

    @Test
    void successfulTest(){
        // Given
        RewardsNotification reward = new RewardsNotification();
        reward.setUserId("USERID");
        reward.setInitiativeId("INITIATIATIVEID");
        reward.setStatus(RewardNotificationStatus.TO_SEND);

        Mockito.when(ibanRepositoryMock.findById("USERIDINITIATIATIVEID")).thenReturn(Mono.just(RewardIban.builder().iban("IBAN").checkIbanOutcome("IBANOUTCOME").build()));

        // When
        RewardsNotification result = service.retrieveIban(reward).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertSame(reward, result);

        Assertions.assertEquals(RewardNotificationStatus.TO_SEND, result.getStatus());
        Assertions.assertNull(result.getRejectionReason());
        Assertions.assertNull(result.getResultCode());

        Mockito.verifyNoMoreInteractions(ibanRepositoryMock, rewardsNotificationRepositoryMock, errorNotifierServiceMock);
    }

    @Test
    void testWhenException(){
        // Given
        RewardsNotification reward = new RewardsNotification();
        reward.setUserId("USERID");
        reward.setInitiativeId("INITIATIATIVEID");
        reward.setStatus(RewardNotificationStatus.TO_SEND);

        Mockito.when(ibanRepositoryMock.findById("USERIDINITIATIATIVEID")).thenReturn(Mono.error(new RuntimeException("DUMMY")));

        // When
        RewardsNotification result = service.retrieveIban(reward).block();

        // Then
        Assertions.assertNull(result);

        Mockito.verifyNoMoreInteractions(ibanRepositoryMock, rewardsNotificationRepositoryMock, errorNotifierServiceMock);
    }
}
