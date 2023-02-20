package it.gov.pagopa.reward.notification.service.csv.out.mapper;

import it.gov.pagopa.reward.notification.dto.mapper.RewardNotificationExport2CsvMapper;
import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationExportCsvDto;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.model.User;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.csv.out.retrieve.Iban2NotifyRetrieverService;
import it.gov.pagopa.reward.notification.service.csv.out.retrieve.User2NotifyRetrieverService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class RewardNotification2ExportRewardNotificationCsvServiceTest {

    @Mock private RewardsNotificationRepository rewardsNotificationRepositoryMock;
    @Mock private Iban2NotifyRetrieverService iban2NotifyRetrieverServiceMock;
    @Mock private User2NotifyRetrieverService user2NotifyRetrieverServiceMock;
    @Mock private RewardNotificationExport2CsvMapper rewardNotificationExport2CsvMapperMock;

    private RewardNotification2ExportCsvService service;

    @BeforeEach
    void init(){
        service = new RewardNotification2ExportCsvServiceImpl(rewardsNotificationRepositoryMock, iban2NotifyRetrieverServiceMock, user2NotifyRetrieverServiceMock, rewardNotificationExport2CsvMapperMock);
    }

    @AfterEach
    void verifyNoMoreInvocations(){
        Mockito.verifyNoMoreInteractions(iban2NotifyRetrieverServiceMock, user2NotifyRetrieverServiceMock, rewardNotificationExport2CsvMapperMock, rewardsNotificationRepositoryMock);
    }

    @Test
    void testNoIban(){
        testIbanNotSuccessful(Mono.empty());
    }
    @Test
    void testIbanException(){
        testIbanNotSuccessful(Mono.error(new RuntimeException("DUMMY")));
    }
    void testIbanNotSuccessful(Mono<RewardsNotification> ibanRetrieveResult){
        // Given
        RewardsNotification reward = new RewardsNotification();
        reward.setRewardCents(1_00L);

        Mockito.when(iban2NotifyRetrieverServiceMock.retrieveIban(Mockito.same(reward))).thenReturn(ibanRetrieveResult);

        // When
        RewardNotificationExportCsvDto result = service.apply(reward).block();

        // Then
        Assertions.assertNull(result);
    }

    @Test
    void testNoUser(){
        testUserNotSuccessful(Mono.empty());
    }
    @Test
    void testUserException(){
        testUserNotSuccessful(Mono.error(new RuntimeException("DUMMY")));
    }
    void testUserNotSuccessful(Mono<Pair<RewardsNotification, User>> userRetrieveResult){
        // Given
        RewardsNotification reward = new RewardsNotification();
        reward.setRewardCents(1_00L);

        Mockito.when(iban2NotifyRetrieverServiceMock.retrieveIban(Mockito.same(reward))).thenReturn(Mono.just(reward));
        Mockito.when(user2NotifyRetrieverServiceMock.retrieveUser(Mockito.same(reward))).thenReturn(userRetrieveResult);

        // When
        RewardNotificationExportCsvDto result = service.apply(reward).block();

        // Then
        Assertions.assertNull(result);
    }

    @Test
    void test() {
        // Given
        RewardsNotification reward = new RewardsNotification();
        reward.setRewardCents(1_00L);
        User user = new User();
        RewardNotificationExportCsvDto expectedCsvLine = new RewardNotificationExportCsvDto();

        Mockito.when(iban2NotifyRetrieverServiceMock.retrieveIban(Mockito.same(reward))).thenReturn(Mono.just(reward));
        Mockito.when(user2NotifyRetrieverServiceMock.retrieveUser(Mockito.same(reward))).thenReturn(Mono.just(Pair.of(reward, user)));
        Mockito.when(rewardNotificationExport2CsvMapperMock.apply(Mockito.same(reward), Mockito.same(user))).thenReturn(expectedCsvLine);

        // When
        RewardNotificationExportCsvDto result = service.apply(reward).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertSame(expectedCsvLine, result);
    }

    @Test
    void testNoRewardAmount(){
        // Given
        RewardsNotification reward = new RewardsNotification();
        reward.setRewardCents(0L);

        Mockito.when(rewardsNotificationRepositoryMock.save(Mockito.same(reward))).thenReturn(Mono.just(reward));

        // When
        RewardNotificationExportCsvDto result = service.apply(reward).block();

        // Then
        Assertions.assertNull(result);

        Assertions.assertEquals(RewardNotificationStatus.SKIPPED, reward.getStatus());
        Assertions.assertNotNull(reward.getExportDate());
    }

}
