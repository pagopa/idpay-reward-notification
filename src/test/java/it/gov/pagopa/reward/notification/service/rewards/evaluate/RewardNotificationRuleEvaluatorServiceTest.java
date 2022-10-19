package it.gov.pagopa.reward.notification.service.rewards.evaluate;

import it.gov.pagopa.reward.notification.dto.mapper.RewardMapper;
import it.gov.pagopa.reward.notification.dto.mapper.RewardMapperTest;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.enums.RewardStatus;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.Rewards;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.ErrorNotifierService;
import it.gov.pagopa.reward.notification.service.rewards.RewardsService;
import it.gov.pagopa.reward.notification.service.rule.RewardNotificationRuleService;
import it.gov.pagopa.reward.notification.test.fakers.RewardNotificationRuleFaker;
import it.gov.pagopa.reward.notification.test.fakers.RewardTransactionDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import reactor.core.publisher.Mono;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class RewardNotificationRuleEvaluatorServiceTest {
    @Mock private RewardNotificationRuleService rewardNotificationRuleServiceMock;
    @Mock private RewardNotificationHandlerFacadeService rewardNotificationHandlerFacadeServiceMock;
    @Mock private RewardsService rewardsServiceMock;
    @Mock private ErrorNotifierService errorNotifierServiceMock;
    @Mock private RewardsNotificationRepository notificationRepositoryMock;

    private final RewardMapper rewardMapper = new RewardMapper();

    private RewardNotificationRuleEvaluatorService service;

    @BeforeEach
    void init() {
        service = new RewardNotificationRuleEvaluatorServiceImpl(
                rewardNotificationRuleServiceMock,
                rewardNotificationHandlerFacadeServiceMock,
                rewardMapper,
                rewardsServiceMock,
                notificationRepositoryMock,
                errorNotifierServiceMock);

        Mockito.when(rewardsServiceMock.save(Mockito.any())).thenAnswer(a->Mono.just(a.getArgument(0)));
    }

    @Test
    void testNoInitiative(){
        // Given
        Mockito.when(rewardNotificationRuleServiceMock.findById(Mockito.any())).thenReturn(Mono.empty());
        String initiativeId = "INITIATIVEID";

        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(1, initiativeId);
        @SuppressWarnings("unchecked") Message<String> message = Mockito.mock(Message.class);

        // When
        Rewards result = service.retrieveAndEvaluate(initiativeId, trx.getRewards().get(initiativeId), trx, message).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(RewardStatus.REJECTED, result.getStatus());

        Mockito.verify(rewardNotificationRuleServiceMock).findById(initiativeId);
        Mockito.verify(errorNotifierServiceMock).notifyRewardResponse(Mockito.same(message), Mockito.eq("[REWARD_NOTIFICATION] Cannot find initiative having id: INITIATIVEID"), Mockito.eq(true), Mockito.notNull());
        Mockito.verify(rewardsServiceMock).save(Mockito.same(result));

        Mockito.verifyNoMoreInteractions(rewardNotificationRuleServiceMock, errorNotifierServiceMock, rewardsServiceMock);
        Mockito.verifyNoInteractions(rewardNotificationHandlerFacadeServiceMock, notificationRepositoryMock);
    }

    @Test
    void testNoNotification(){

        // Given
        String initiativeId = "INITIATIVEID";
        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(1, initiativeId);
        RewardNotificationRule rule = RewardNotificationRuleFaker.mockInstance(1);

        Mockito.when(rewardNotificationRuleServiceMock.findById(Mockito.any())).thenReturn(Mono.just(rule));
        Mockito.when(rewardNotificationHandlerFacadeServiceMock.configureRewardNotification(Mockito.same(trx), Mockito.same(rule), Mockito.same(trx.getRewards().get(initiativeId))))
                .thenReturn(Mono.empty());

        @SuppressWarnings("unchecked") Message<String> message = Mockito.mock(Message.class);

        // When
        Rewards result = service.retrieveAndEvaluate(initiativeId, trx.getRewards().get(initiativeId), trx, message).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(RewardStatus.REJECTED, result.getStatus());

        Mockito.verify(rewardNotificationRuleServiceMock).findById(initiativeId);
        Mockito.verify(rewardNotificationHandlerFacadeServiceMock).configureRewardNotification(Mockito.same(trx), Mockito.same(rule), Mockito.same(trx.getRewards().get(initiativeId)));
        Mockito.verify(errorNotifierServiceMock).notifyRewardResponse(Mockito.same(message), Mockito.eq("[REWARD_NOTIFICATION] Cannot configure notificationId for reward: IDTRXACQUIRER1ACQUIRERCODE120220525T05101000ACQUIRERID1_INITIATIVEID"), Mockito.eq(true), Mockito.notNull());
        Mockito.verify(rewardsServiceMock).save(Mockito.same(result));

        Mockito.verifyNoMoreInteractions(rewardNotificationRuleServiceMock, rewardNotificationHandlerFacadeServiceMock, errorNotifierServiceMock, rewardsServiceMock);
        Mockito.verifyNoInteractions(notificationRepositoryMock);
    }

    @Test
    void testSuccessful(){
        // Given
        String initiativeId = "INITIATIVEID";
        String notificationId = "NOTIFICATIONID";
        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(1, initiativeId);
        RewardNotificationRule rule = RewardNotificationRuleFaker.mockInstance(1);

        Mockito.when(rewardNotificationRuleServiceMock.findById(Mockito.any())).thenReturn(Mono.just(rule));
        Mockito.when(rewardNotificationHandlerFacadeServiceMock.configureRewardNotification(Mockito.same(trx), Mockito.same(rule), Mockito.same(trx.getRewards().get(initiativeId))))
                .thenReturn(Mono.just(
                        RewardsNotification.builder()
                                .id(notificationId)
                                .trxIds(List.of(trx.getId()))
                                .build()));
        Mockito.when(notificationRepositoryMock.save(Mockito.any())).thenAnswer(a->Mono.just(a.getArgument(0)));

        @SuppressWarnings("unchecked") Message<String> message = Mockito.mock(Message.class);

        // When
        Rewards result = service.retrieveAndEvaluate(initiativeId, trx.getRewards().get(initiativeId), trx, message).block();

        // Then
        Assertions.assertNotNull(result);
        RewardMapperTest.checkCommonFields(result, initiativeId, trx, rule, notificationId);

        Mockito.verify(rewardNotificationRuleServiceMock).findById(initiativeId);
        Mockito.verify(rewardNotificationHandlerFacadeServiceMock).configureRewardNotification(Mockito.same(trx), Mockito.same(rule), Mockito.same(trx.getRewards().get(initiativeId)));
        Mockito.verify(rewardsServiceMock).save(Mockito.same(result));
        Mockito.verify(notificationRepositoryMock).save(Mockito.argThat(n -> n.getTrxIds().contains(result.getTrxId())));

        Mockito.verifyNoMoreInteractions(rewardNotificationRuleServiceMock, rewardNotificationHandlerFacadeServiceMock, rewardsServiceMock, notificationRepositoryMock);
        Mockito.verifyNoInteractions(errorNotifierServiceMock);
    }
}
