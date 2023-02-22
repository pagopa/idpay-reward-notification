package it.gov.pagopa.reward.notification.service.iban.outcome.recovery;

import it.gov.pagopa.reward.notification.model.RewardIban;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class RecoverIbanKoServiceTest {

    @Mock private NeverExportedDiscardedRewardNotificationService neverExportedServiceMock;
    @Mock private CompletedKoDiscardedRewardNotificationService completedKoServiceMock;

    private RecoverIbanKoService service;

    @BeforeEach
    void init(){
        service = new RecoverIbanKoServiceImpl(neverExportedServiceMock, completedKoServiceMock);
    }

    private void executeAndCheckResult(RewardIban rewardIban) {
        // When
        RewardIban result = service.recover(rewardIban).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertSame(rewardIban, result);
    }

    @Test
    void test(){
        // Given
        RewardIban rewardIban = new RewardIban();

        Mockito.when(neverExportedServiceMock.handleNeverExportedDiscardedRewardNotification(Mockito.same(rewardIban))).thenReturn(Mono.empty());
        Mockito.when(completedKoServiceMock.handleCompletedKoDiscardedRewardNotification(Mockito.same(rewardIban))).thenReturn(Mono.empty());

        // When
        executeAndCheckResult(rewardIban);
    }

    @Test
    void testWhenErrorOnNeverExported(){
        // Given
        RewardIban rewardIban = new RewardIban();

        Mockito.when(neverExportedServiceMock.handleNeverExportedDiscardedRewardNotification(Mockito.same(rewardIban))).thenReturn(Mono.error(new RuntimeException("DUMMY")));

        executeAndCheckResult(rewardIban);
    }

    @Test
    void testWhenErrorOnCompletedKo(){
        // Given
        RewardIban rewardIban = new RewardIban();

        Mockito.when(neverExportedServiceMock.handleNeverExportedDiscardedRewardNotification(Mockito.same(rewardIban))).thenReturn(Mono.empty());
        Mockito.when(completedKoServiceMock.handleCompletedKoDiscardedRewardNotification(Mockito.same(rewardIban))).thenReturn(Mono.error(new RuntimeException("DUMMY")));

        // When
        executeAndCheckResult(rewardIban);
    }
}
