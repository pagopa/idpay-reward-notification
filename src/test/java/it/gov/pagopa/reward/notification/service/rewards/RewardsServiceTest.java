package it.gov.pagopa.reward.notification.service.rewards;

import it.gov.pagopa.reward.notification.dto.mapper.RewardMapper;
import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.enums.RewardStatus;
import it.gov.pagopa.reward.notification.model.Rewards;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.repository.RewardsRepository;
import it.gov.pagopa.reward.notification.test.fakers.RewardNotificationRuleFaker;
import it.gov.pagopa.reward.notification.test.fakers.RewardTransactionDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class RewardsServiceTest {

    @Mock private RewardsRepository rewardsRepositoryMock;
    @Mock private RewardsNotificationRepository rewardsNotificationRepositoryMock;

    private final RewardMapper rewardMapper = new RewardMapper();
    private RewardsService rewardsService;

    @BeforeEach
    void init(){
        rewardsService = new RewardsServiceImpl(rewardsRepositoryMock, rewardsNotificationRepositoryMock, rewardMapper);
    }

    @Test
    void checkDuplicateRewardOk() {
        // Given
        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(1);

        Mockito.when(rewardsRepositoryMock.findById(trx.getId()+"_INITIATIVEID")).thenReturn(Mono.empty());

        // When
        RewardTransactionDTO result = rewardsService.checkDuplicateReward(trx,"INITIATIVEID").block();

        // Then
        Assertions.assertSame(trx, result);

        Mockito.verifyNoInteractions(rewardsNotificationRepositoryMock);
    }

    @Test
    void checkDuplicateTransactionsKo() {
        // Given
        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(1);

        Rewards rewardDuplicate = rewardMapper.apply("INITIATIVEID", new Reward(BigDecimal.ONE), trx, RewardNotificationRuleFaker.mockInstance(1), "NOTIFICATIONID");
        Mockito.when(rewardsRepositoryMock.findById(trx.getId()+"_INITIATIVEID")).thenReturn(Mono.just(rewardDuplicate));

        Mockito.when(rewardsNotificationRepositoryMock.findById(rewardDuplicate.getNotificationId()))
                .thenReturn(Mono.just(
                        RewardsNotification.builder()
                                .trxIds(List.of(trx.getId()))
                                .build()
                ));

        // When
        RewardTransactionDTO result = rewardsService.checkDuplicateReward(trx, "INITIATIVEID").block();

        // Then
        Assertions.assertNull(result);
    }

    @Test
    void checkDuplicateTransactionsRejectedOk() {
        // Given
        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(1);

        Rewards rewardDuplicate = rewardMapper.apply("INITIATIVEID", new Reward(BigDecimal.ONE), trx, RewardNotificationRuleFaker.mockInstance(1), "NOTIFICATIONID");
        rewardDuplicate.setStatus(RewardStatus.REJECTED);
        Mockito.when(rewardsRepositoryMock.findById(trx.getId()+"_INITIATIVEID")).thenReturn(Mono.just(rewardDuplicate));

        // When
        RewardTransactionDTO result = rewardsService.checkDuplicateReward(trx,"INITIATIVEID").block();

        // Then
        Assertions.assertSame(trx, result);

        Mockito.verifyNoInteractions(rewardsNotificationRepositoryMock);
    }

    @Test
    void checkDuplicateTransactionsNotNotificationIdOk() {
        // Given
        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(1);

        Rewards rewardDuplicate = rewardMapper.apply("INITIATIVEID", new Reward(BigDecimal.ONE), trx, RewardNotificationRuleFaker.mockInstance(1), "NOTIFICATIONID");
        rewardDuplicate.setNotificationId(null);
        Mockito.when(rewardsRepositoryMock.findById(trx.getId()+"_INITIATIVEID")).thenReturn(Mono.just(rewardDuplicate));

        // When
        RewardTransactionDTO result = rewardsService.checkDuplicateReward(trx, "INITIATIVEID").block();

        // Then
        Assertions.assertSame(trx, result);

        Mockito.verifyNoInteractions(rewardsNotificationRepositoryMock);
    }

    @Test
    void checkDuplicateTransactionsNotElaboratedOk() {
        // Given
        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(1);

        Rewards rewardDuplicate = rewardMapper.apply("INITIATIVEID", new Reward(BigDecimal.ONE), trx, RewardNotificationRuleFaker.mockInstance(1), "NOTIFICATIONID");
        Mockito.when(rewardsRepositoryMock.findById(trx.getId()+"_INITIATIVEID")).thenReturn(Mono.just(rewardDuplicate));

        Mockito.when(rewardsNotificationRepositoryMock.findById(rewardDuplicate.getNotificationId())).thenReturn(Mono.just(new RewardsNotification()));

        // When
        RewardTransactionDTO result = rewardsService.checkDuplicateReward(trx, "INITIATIVEID").block();

        // Then
        Assertions.assertSame(trx, result);
    }

    @Test
    void checkDuplicateTransactionsNotificationNotExistsOk() {
        // Given
        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(1);

        Rewards rewardDuplicate = rewardMapper.apply("INITIATIVEID", new Reward(BigDecimal.ONE), trx, RewardNotificationRuleFaker.mockInstance(1), "NOTIFICATIONID");
        Mockito.when(rewardsRepositoryMock.findById(trx.getId()+"_INITIATIVEID")).thenReturn(Mono.just(rewardDuplicate));

        Mockito.when(rewardsNotificationRepositoryMock.findById(rewardDuplicate.getNotificationId())).thenReturn(Mono.empty());

        // When
        RewardTransactionDTO result = rewardsService.checkDuplicateReward(trx, "INITIATIVEID").block();

        // Then
        Assertions.assertSame(trx, result);
    }
}
