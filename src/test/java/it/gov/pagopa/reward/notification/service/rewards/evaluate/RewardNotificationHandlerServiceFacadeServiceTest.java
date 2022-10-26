package it.gov.pagopa.reward.notification.service.rewards.evaluate;

import it.gov.pagopa.reward.notification.dto.rule.AccumulatedAmountDTO;
import it.gov.pagopa.reward.notification.dto.rule.TimeParameterDTO;
import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.notify.RewardNotificationBudgetExhaustedHandlerServiceImpl;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.notify.RewardNotificationHandlerService;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.notify.RewardNotificationTemporalHandlerServiceImpl;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.notify.RewardNotificationThresholdHandlerServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@ExtendWith(MockitoExtension.class)
class RewardNotificationHandlerServiceFacadeServiceTest {
    @Mock private RewardNotificationTemporalHandlerServiceImpl temporalHandlerMock;
    @Mock private RewardNotificationBudgetExhaustedHandlerServiceImpl budgetExhaustedHandlerMock;
    @Mock private RewardNotificationThresholdHandlerServiceImpl thresholdHandlerMock;

    private RewardNotificationHandlerFacadeService service;

    @BeforeEach
    void init(){
        service = new RewardNotificationHandlerFacadeServiceImpl(temporalHandlerMock, budgetExhaustedHandlerMock, thresholdHandlerMock);
    }

    @Test
    void testTemporalRule(){
        RewardNotificationRule rule = new RewardNotificationRule();
        rule.setTimeParameter(new TimeParameterDTO());
        rule.setAccumulatedAmount(new AccumulatedAmountDTO());

        test(rule, temporalHandlerMock);
    }

    @Test
    void testExhaustedBudgetRule(){
        RewardNotificationRule rule = new RewardNotificationRule();
        rule.setAccumulatedAmount(new AccumulatedAmountDTO());
        rule.getAccumulatedAmount().setAccumulatedType(AccumulatedAmountDTO.AccumulatedTypeEnum.BUDGET_EXHAUSTED);

        test(rule, budgetExhaustedHandlerMock);
    }

    @Test
    void testThresholdRule(){
        RewardNotificationRule rule = new RewardNotificationRule();
        rule.setAccumulatedAmount(new AccumulatedAmountDTO());
        rule.getAccumulatedAmount().setAccumulatedType(AccumulatedAmountDTO.AccumulatedTypeEnum.THRESHOLD_REACHED);
        rule.getAccumulatedAmount().setRefundThreshold(BigDecimal.ONE);

        test(rule, thresholdHandlerMock);
    }

    private void test(RewardNotificationRule rule, RewardNotificationHandlerService handlerServiceMock){
        // Given
        RewardTransactionDTO trx = new RewardTransactionDTO();
        Reward reward = new Reward();

        Mono<RewardsNotification> expectedResult = Mono.just(new RewardsNotification());

        Mockito.when(handlerServiceMock.handle(Mockito.same(trx), Mockito.same(rule), Mockito.same(reward)))
                .thenReturn(expectedResult);

        // When
        Mono<RewardsNotification> result = service.configureRewardNotification(trx, rule, reward);

        // Then
        Assertions.assertSame(expectedResult, result);
        Mockito.verify(handlerServiceMock).handle(Mockito.same(trx), Mockito.same(rule), Mockito.same(reward));
        Mockito.verifyNoMoreInteractions(handlerServiceMock);

        if(handlerServiceMock != temporalHandlerMock){
            Mockito.verifyNoInteractions(temporalHandlerMock);
        }
        if(handlerServiceMock != budgetExhaustedHandlerMock){
            Mockito.verifyNoInteractions(budgetExhaustedHandlerMock);
        }
        if(handlerServiceMock != thresholdHandlerMock){
            Mockito.verifyNoInteractions(thresholdHandlerMock);
        }
    }

    @Test
    void testNotValidRule(){
        // Given
        RewardNotificationRule rule = new RewardNotificationRule();

        testErrorCondition(rule, "[REWARD_NOTIFICATION] [INVALID_INITIATIVE] Not valid rule null");
    }

    @Test
    void testNotValidThresholdRule(){
        // Given
        RewardNotificationRule rule = new RewardNotificationRule();
        rule.setAccumulatedAmount(new AccumulatedAmountDTO());
        rule.getAccumulatedAmount().setAccumulatedType(AccumulatedAmountDTO.AccumulatedTypeEnum.THRESHOLD_REACHED);

        testErrorCondition(rule, "[REWARD_NOTIFICATION] [INVALID_INITIATIVE] Not valid threshold rule null");
    }

    private void testErrorCondition(RewardNotificationRule rule, String expectedMessage){
        // When
        Mono<RewardsNotification> result = service.configureRewardNotification(new RewardTransactionDTO(), rule, null);

        // Then
        try{
            result.block();
            Assertions.fail("");
        } catch (IllegalStateException e){
            Assertions.assertEquals(expectedMessage, e.getMessage());
        }
    }
}
