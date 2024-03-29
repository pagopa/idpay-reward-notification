package it.gov.pagopa.reward.notification.service.csv.out.retrieve;

import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.model.User;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.UserService;
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
class User2NotifyRetrieverServiceTest {

    @Mock private UserService userServiceMock;
    @Mock private RewardsNotificationRepository rewardsNotificationRepositoryMock;
    @Mock private RewardNotificationNotifierService errorNotifierServiceMock;

    private User2NotifyRetrieverService service;

    @BeforeEach
    void init(){
        service = new User2NotifyRetrieverServiceImpl(userServiceMock, rewardsNotificationRepositoryMock, errorNotifierServiceMock);
    }

    @Test
    void noUserTest(){
        // Given
        RewardsNotification reward = new RewardsNotification();
        reward.setBeneficiaryId("USERID");
        reward.setStatus(RewardNotificationStatus.TO_SEND);

        Mockito.when(userServiceMock.getUserInfo("USERID")).thenReturn(Mono.empty());
        Mockito.when(rewardsNotificationRepositoryMock.save(Mockito.same(reward))).thenReturn(Mono.just(reward));
        Mockito.when(errorNotifierServiceMock.notify(Mockito.same(reward), Mockito.eq(0L))).thenReturn(Mono.just(reward));

        // When
        Pair<RewardsNotification, User> result = service.retrieveUser(reward).block();

        // Then
        Assertions.assertNull(result);

        Assertions.assertEquals(RewardNotificationStatus.ERROR, reward.getStatus());
        Assertions.assertEquals(ExportCsvConstants.EXPORT_REJECTION_REASON_CF_NOT_FOUND, reward.getRejectionReason());
        Assertions.assertEquals(ExportCsvConstants.EXPORT_REJECTION_REASON_CF_NOT_FOUND, reward.getResultCode());

        Mockito.verifyNoMoreInteractions(userServiceMock, rewardsNotificationRepositoryMock, errorNotifierServiceMock);
    }

    @Test
    void successfulTest(){
        // Given
        RewardsNotification reward = new RewardsNotification();
        reward.setBeneficiaryId("USERID");
        reward.setStatus(RewardNotificationStatus.TO_SEND);

        User expectedUserRetrieved = User.builder().fiscalCode("CF").build();
        Mockito.when(userServiceMock.getUserInfo("USERID")).thenReturn(Mono.just(expectedUserRetrieved));

        // When
        Pair<RewardsNotification, User> result = service.retrieveUser(reward).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertSame(reward, result.getKey());
        Assertions.assertSame(expectedUserRetrieved, result.getValue());

        Assertions.assertEquals(RewardNotificationStatus.TO_SEND, result.getKey().getStatus());
        Assertions.assertNull(result.getKey().getRejectionReason());
        Assertions.assertNull(result.getKey().getResultCode());

        Mockito.verifyNoMoreInteractions(userServiceMock, rewardsNotificationRepositoryMock, errorNotifierServiceMock);
    }

    @Test
    void testWhenException(){
        // Given
        RewardsNotification reward = new RewardsNotification();
        reward.setBeneficiaryId("USERID");
        reward.setStatus(RewardNotificationStatus.TO_SEND);

        Mockito.when(userServiceMock.getUserInfo("USERID")).thenReturn(Mono.error(new RuntimeException("DUMMY")));

        // When
        Pair<RewardsNotification, User> result = service.retrieveUser(reward).block();

        // Then
        Assertions.assertNull(result);

        Mockito.verifyNoMoreInteractions(userServiceMock, rewardsNotificationRepositoryMock, errorNotifierServiceMock);
    }
}
