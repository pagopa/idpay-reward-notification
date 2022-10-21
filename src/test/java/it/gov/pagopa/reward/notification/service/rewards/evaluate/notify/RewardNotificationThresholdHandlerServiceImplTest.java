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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class RewardNotificationThresholdHandlerServiceImplTest {

    public static final LocalDate TOMORROW = LocalDate.now().plusDays(1);
    public static final LocalDate NEXT_SUNDAY = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
    public static final String NOTIFICATION_DAY_TOMORROW = "TOMORROW";
    public static final String NOTIFICATION_DAY_NEXTDAYOFWEEK = "NEXT_SUNDAY";

    @Mock
    protected RewardsNotificationRepository repositoryMock;
    @Spy
    protected RewardsNotificationMapper mapperSpy;

    protected RewardNotificationThresholdHandlerServiceImpl buildService(String notificationDay){
        return new RewardNotificationThresholdHandlerServiceImpl(notificationDay, repositoryMock, mapperSpy);
    }

    protected RewardNotificationRule buildRule() {
        RewardNotificationRule rule = new RewardNotificationRule();
        rule.setInitiativeId("INITIATIVEID");
        rule.setAccumulatedAmount(new AccumulatedAmountDTO());
        rule.getAccumulatedAmount().setAccumulatedType(AccumulatedAmountDTO.AccumulatedTypeEnum.THRESHOLD_REACHED);
        rule.getAccumulatedAmount().setRefundThresholdCents(500L);
        return rule;
    }

    protected DepositType getExpectedDepositType() {
        return DepositType.PARTIAL;
    }

    @Test
    void testNotificationDayConfiguration() throws IllegalAccessException {
        try{
            buildService(null);
            Assertions.fail("Expected exception");
        } catch (IllegalArgumentException e){
            // Do nothing
        }

        try{
            buildService("INVALID_CONFIG");
            Assertions.fail("Expected exception");
        } catch (IllegalArgumentException e){
            // Do nothing
        }

        try{
            buildService("NEXT_INVALID_DAY_OF_WEEK");
            Assertions.fail("Expected exception");
        } catch (IllegalArgumentException e){
            // Do nothing
        }

        Field fieldNotificateNextDay = ReflectionUtils.findField(RewardNotificationThresholdHandlerServiceImpl.class, "notificateNextDay");
        Field fieldNotificateNextDayOfWeek = ReflectionUtils.findField(RewardNotificationThresholdHandlerServiceImpl.class, "notificateNextDayOfWeek");
        Assertions.assertNotNull(fieldNotificateNextDay);
        Assertions.assertNotNull(fieldNotificateNextDayOfWeek);
        fieldNotificateNextDay.setAccessible(true);
        fieldNotificateNextDayOfWeek.setAccessible(true);

        RewardNotificationThresholdHandlerServiceImpl configurationTomorrow = buildService(NOTIFICATION_DAY_TOMORROW);
        Assertions.assertTrue(fieldNotificateNextDay.getBoolean(configurationTomorrow));
        Assertions.assertNull(fieldNotificateNextDayOfWeek.get(configurationTomorrow));

        RewardNotificationThresholdHandlerServiceImpl configurationNextDayOfWeek = buildService(NOTIFICATION_DAY_NEXTDAYOFWEEK);
        Assertions.assertFalse(fieldNotificateNextDay.getBoolean(configurationNextDayOfWeek));
        Assertions.assertEquals(DayOfWeek.SUNDAY, fieldNotificateNextDayOfWeek.get(configurationNextDayOfWeek));
    }

    @Test void testHandleNewNotifyCharge_TOMORROW(){
        testHandleNewNotifyNotOverflowing(false, NOTIFICATION_DAY_TOMORROW);}
    @Test void testHandleNewNotifyCharge_NEXTDAYOFWEEK(){
        testHandleNewNotifyNotOverflowing(false, NOTIFICATION_DAY_NEXTDAYOFWEEK);}

    @Test void testHandleNewNotifyRefundNoFutureNotification_TOMORROW(){
        testHandleNewNotifyNotOverflowing(true, NOTIFICATION_DAY_TOMORROW);}
    @Test void testHandleNewNotifyRefundNoFutureNotification_NEXTDAYOFWEEK(){
        testHandleNewNotifyNotOverflowing(true, NOTIFICATION_DAY_NEXTDAYOFWEEK);}


    void testHandleNewNotifyNotOverflowing(boolean isRefund, String notificationDay){
        // Given
        RewardNotificationThresholdHandlerServiceImpl service = buildService(notificationDay);

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
        Assertions.assertEquals(getExpectedDepositType(), result.getDepositType());

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

    @Test void testHandleNewNotifyRefundWithFutureNotificationNotOverflowing_TOMORROW(){testHandleNewNotifyRefundWithFutureNotification(false, NOTIFICATION_DAY_TOMORROW);}
    @Test void testHandleNewNotifyRefundWithFutureNotificationNotOverflowing_NEXTDAYOFWEEK(){testHandleNewNotifyRefundWithFutureNotification(false, NOTIFICATION_DAY_NEXTDAYOFWEEK);}

    @Test void testHandleNewNotifyRefundWithFutureNotificationStillOverflowing_TOMORROW(){testHandleNewNotifyRefundWithFutureNotification(true, NOTIFICATION_DAY_TOMORROW);}
    @Test void testHandleNewNotifyRefundWithFutureNotificationStillOverflowing_NEXTDAYOFWEEK(){testHandleNewNotifyRefundWithFutureNotification(true, NOTIFICATION_DAY_NEXTDAYOFWEEK);}

    void testHandleNewNotifyRefundWithFutureNotification(boolean isStillOverflowing, String notificationDay){
        // Given
        RewardNotificationThresholdHandlerServiceImpl service = buildService(notificationDay);

        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(0);
        RewardNotificationRule rule = buildRule();
        rule.setInitiativeId("INITIATIVEID");
        rule.setEndDate(LocalDate.now());
        Reward reward = testHandleNewNotifyRefundWithFutureNotification_buildOverFlowingReward(isStillOverflowing);

        LocalDate notificationDate = NOTIFICATION_DAY_TOMORROW.equals(notificationDay) ? TOMORROW : NEXT_SUNDAY;
        long expectedProgressive = 5L;
        RewardsNotification expectedResult = RewardsNotificationFaker.mockInstance(0, rule.getInitiativeId(), notificationDate);
        expectedResult.setProgressive(expectedProgressive);
        expectedResult.setRewardCents(600L);
        expectedResult.setNotificationDate(notificationDate);
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

        Assertions.assertEquals(testHandleNewNotifyRefundWithFutureNotification_expectedReward(isStillOverflowing), result.getRewardCents());
        Assertions.assertEquals(expectedProgressive, result.getProgressive());
        Assertions.assertEquals(DepositType.FINAL, result.getDepositType()); // because the rule end today
        Assertions.assertEquals(List.of("TRXID", trx.getId()), result.getTrxIds());
        Assertions.assertEquals(isStillOverflowing? notificationDate : null, result.getNotificationDate());

        Mockito.verify(service).handle(Mockito.same(trx), Mockito.same(rule), Mockito.same(reward));

        Mockito.verifyNoMoreInteractions(repositoryMock, mapperSpy);
    }

    protected Reward testHandleNewNotifyRefundWithFutureNotification_buildOverFlowingReward(boolean isStillOverflowing) {
        return new Reward(BigDecimal.valueOf(isStillOverflowing ? -0.5 : -3));
    }

    protected long testHandleNewNotifyRefundWithFutureNotification_expectedReward(boolean isStillOverflowing) {
        return isStillOverflowing ? 550L : 300L;
    }

    @Test
    void testHandleUpdateNotifyOverflowingThreshold(){
        // Given
        RewardNotificationThresholdHandlerServiceImpl service = buildService(NOTIFICATION_DAY_TOMORROW);

        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(0);
        RewardNotificationRule rule = buildRule();
        rule.setInitiativeId("INITIATIVEID");
        rule.setEndDate(LocalDate.now().plusDays(1));
        Reward reward = testHandleUpdateNotifyOverflowingThreshold_buildOverFlowingReward();

        LocalDate expectedNotificationDate = TOMORROW;
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
        Assertions.assertEquals(getExpectedDepositType(), result.getDepositType());
        Assertions.assertEquals(List.of("TRXID", trx.getId()), result.getTrxIds());
        Assertions.assertEquals(expectedNotificationDate, result.getNotificationDate());

        Mockito.verify(service).handle(Mockito.same(trx), Mockito.same(rule), Mockito.same(reward));

        Mockito.verifyNoMoreInteractions(repositoryMock, mapperSpy);
    }

    protected Reward testHandleUpdateNotifyOverflowingThreshold_buildOverFlowingReward() {
        return new Reward(BigDecimal.valueOf(3));
    }
}
