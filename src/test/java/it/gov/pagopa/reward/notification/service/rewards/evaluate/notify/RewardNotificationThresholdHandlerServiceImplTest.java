package it.gov.pagopa.reward.notification.service.rewards.evaluate.notify;

import it.gov.pagopa.reward.notification.dto.mapper.RewardsNotificationMapper;
import it.gov.pagopa.reward.notification.dto.rule.AccumulatedAmountDTO;
import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.enums.DepositType;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.test.fakers.RewardTransactionDTOFaker;
import it.gov.pagopa.reward.notification.test.fakers.RewardsNotificationFaker;
import it.gov.pagopa.reward.notification.test.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class RewardNotificationThresholdHandlerServiceImplTest {

    @Mock
    private RewardsNotificationRepository repositoryMock;
    @Spy
    private RewardsNotificationMapper mapperSpy;

    private RewardNotificationThresholdHandlerServiceImpl service;

    @BeforeEach
    void init(){
        service = new RewardNotificationThresholdHandlerServiceImpl(repositoryMock, mapperSpy);
    }

    private static RewardNotificationRule buildRule() {
        RewardNotificationRule rule = new RewardNotificationRule();
        rule.setInitiativeId("INITIATIVEID");
        rule.setAccumulatedAmount(new AccumulatedAmountDTO());
        rule.getAccumulatedAmount().setRefundThresholdCents(500L);
        return rule;
    }

    @Test
    void testHandleNewNotifyCharge(){
        testHandleNewNotify(false);
    }

    @Test
    void testHandleNewNotifyRefundNoFutureNotification(){
        testHandleNewNotify(true);
    }
    void testHandleNewNotify(boolean isRefund){
        // Given
        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(0);
        RewardNotificationRule rule = buildRule();
        Reward reward = new Reward(BigDecimal.valueOf(isRefund? -3 : 3));

        RewardsNotification[] expectedResult = new RewardsNotification[]{null};
        long expectedProgressive = 5L;

        String expectedNotificationId = "USERID0_INITIATIVEID_%d".formatted(expectedProgressive);

        Mockito.when(repositoryMock.findByUserIdAndInitiativeIdAndNotificationDate(trx.getUserId(), rule.getInitiativeId(), null)).thenReturn(Flux.empty());
        Mockito.doAnswer(a ->{
                    expectedResult[0]=(RewardsNotification)a.callRealMethod();
                    return expectedResult[0];
                })
                .when(mapperSpy)
                .apply(Mockito.any(), Mockito.any(), Mockito.anyLong(), Mockito.any(), Mockito.any());

        Mockito.when(repositoryMock.count(Mockito.any())).thenReturn(Mono.just(expectedProgressive - 1));

        if(isRefund){
            Mockito.when(repositoryMock.findByUserIdAndInitiativeIdAndNotificationDateGreaterThan(trx.getUserId(), rule.getInitiativeId(), LocalDate.now())).thenReturn(Flux.empty());
        }

        service = Mockito.spy(service);

        // When
        RewardsNotification result = service.handle(trx, rule, reward).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertSame(expectedResult[0], result);

        Assertions.assertEquals(expectedNotificationId, result.getId());
        Assertions.assertNull(result.getNotificationDate());
        Assertions.assertEquals(isRefund? -300L : 300L, result.getRewardCents());
        Assertions.assertEquals(expectedProgressive, result.getProgressive());
        Assertions.assertEquals(List.of(trx.getId()), result.getTrxIds());
        Assertions.assertEquals(DepositType.PARTIAL, result.getDepositType());

        Mockito.verify(service).handle(Mockito.same(trx), Mockito.same(rule), Mockito.same(reward));
        Mockito.verify(repositoryMock).count(Mockito.argThat(i->{
            RewardsNotification query = i.getProbe();
            Assertions.assertEquals(rule.getInitiativeId(), query.getInitiativeId());
            Assertions.assertEquals(trx.getUserId(), query.getUserId());
            TestUtils.checkNullFields(query, "userId", "initiativeId");
            return true;
        }));
        Mockito.verify(mapperSpy).apply(Mockito.eq("USERID0_INITIATIVEID"), Mockito.isNull(), Mockito.eq(expectedProgressive), Mockito.same(trx), Mockito.same(rule));

        Mockito.verifyNoMoreInteractions(repositoryMock, mapperSpy);
    }

    @Test
    void testHandleNewNotifyRefundWithFutureNotificationNotOverflowing(){
        testHandleNewNotifyRefundWithFutureNotification(false);
    }
    @Test
    void testHandleNewNotifyRefundWithFutureNotificationStillOverflowing(){
        testHandleNewNotifyRefundWithFutureNotification(true);
    }
    void testHandleNewNotifyRefundWithFutureNotification(boolean isStillOverflowing){
        // Given
        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(0);
        RewardNotificationRule rule = buildRule();
        rule.setInitiativeId("INITIATIVEID");
        rule.setEndDate(LocalDate.now());
        Reward reward = new Reward(BigDecimal.valueOf(isStillOverflowing? -0.5 : -3));

        LocalDate notificationDate = LocalDate.now().plusDays(1);
        long expectedProgressive = 5L;
        RewardsNotification expectedResult = RewardsNotificationFaker.mockInstance(0, rule.getInitiativeId(), notificationDate);
        expectedResult.setProgressive(expectedProgressive);
        expectedResult.setRewardCents(600L);
        expectedResult.setNotificationDate(LocalDate.now().plusDays(1));
        expectedResult.getTrxIds().add("TRXID");

        Mockito.when(repositoryMock.findByUserIdAndInitiativeIdAndNotificationDate(trx.getUserId(), rule.getInitiativeId(), null)).thenReturn(Flux.empty());
        Mockito.when(repositoryMock.count(Mockito.any())).thenReturn(Mono.empty());

        Mockito.when(repositoryMock.findByUserIdAndInitiativeIdAndNotificationDateGreaterThan(trx.getUserId(), rule.getInitiativeId(), LocalDate.now())).thenReturn(Flux.just(expectedResult));

        service = Mockito.spy(service);

        // When
        RewardsNotification result = service.handle(trx, rule, reward).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertSame(expectedResult, result);

        Assertions.assertEquals(isStillOverflowing? 550L : 300L, result.getRewardCents());
        Assertions.assertEquals(expectedProgressive, result.getProgressive());
        Assertions.assertEquals(DepositType.FINAL, result.getDepositType());
        Assertions.assertEquals(List.of("TRXID", trx.getId()), result.getTrxIds());
        Assertions.assertEquals(isStillOverflowing? notificationDate : null, result.getNotificationDate());

        Mockito.verify(service).handle(Mockito.same(trx), Mockito.same(rule), Mockito.same(reward));

        Mockito.verifyNoMoreInteractions(repositoryMock, mapperSpy);
    }

    @Test
    void testHandleUpdateNotifyOverflowingThreshold(){
        // Given
        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(0);
        RewardNotificationRule rule = buildRule();
        rule.setInitiativeId("INITIATIVEID");
        rule.setEndDate(LocalDate.now());
        Reward reward = new Reward(BigDecimal.valueOf(3));

        LocalDate expectedNotificationDate = LocalDate.now().plusDays(1);
        long expectedProgressive = 5L;
        RewardsNotification expectedResult = RewardsNotificationFaker.mockInstance(0, rule.getInitiativeId(), expectedNotificationDate);
        expectedResult.setProgressive(expectedProgressive);
        expectedResult.setRewardCents(200L);
        expectedResult.getTrxIds().add("TRXID");

        Mockito.when(repositoryMock.findByUserIdAndInitiativeIdAndNotificationDate(trx.getUserId(), rule.getInitiativeId(), null)).thenReturn(Flux.just(expectedResult));
        Mockito.when(repositoryMock.count(Mockito.any())).thenReturn(Mono.empty());

        service = Mockito.spy(service);

        // When
        RewardsNotification result = service.handle(trx, rule, reward).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertSame(expectedResult, result);

        Assertions.assertEquals(500L, result.getRewardCents());
        Assertions.assertEquals(expectedProgressive, result.getProgressive());
        Assertions.assertEquals(DepositType.FINAL, result.getDepositType());
        Assertions.assertEquals(List.of("TRXID", trx.getId()), result.getTrxIds());
        Assertions.assertEquals(expectedNotificationDate, result.getNotificationDate());

        Mockito.verify(service).handle(Mockito.same(trx), Mockito.same(rule), Mockito.same(reward));

        Mockito.verifyNoMoreInteractions(repositoryMock, mapperSpy);
    }
}
