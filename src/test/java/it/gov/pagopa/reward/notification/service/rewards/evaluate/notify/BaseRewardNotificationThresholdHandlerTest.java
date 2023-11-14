package it.gov.pagopa.reward.notification.service.rewards.evaluate.notify;

import it.gov.pagopa.reward.notification.dto.mapper.RewardsNotificationMapper;
import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.enums.DepositType;
import it.gov.pagopa.reward.notification.enums.InitiativeRewardType;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.test.fakers.RewardTransactionDTOFaker;
import it.gov.pagopa.reward.notification.test.fakers.RewardsNotificationFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
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
abstract class BaseRewardNotificationThresholdHandlerTest {

    public static final LocalDate TOMORROW = LocalDate.now().plusDays(1);
    public static final LocalDate NEXT_MONDAY = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    public static final String NOTIFICATION_DAY_TOMORROW = "TOMORROW";
    public static final String NOTIFICATION_DAY_NEXTDAYOFWEEK = "NEXT_MONDAY";

    @Mock
    protected RewardsNotificationRepository repositoryMock;
    @Spy
    protected RewardsNotificationMapper mapperSpy;

    protected abstract BaseRewardNotificationThresholdBasedHandler buildService(String notificationDay);

    protected abstract RewardNotificationRule buildRule();

    protected abstract DepositType getExpectedDepositType();

    @Test
    void testNotificationDayConfiguration() throws IllegalAccessException {
        try {
            buildService(null);
            Assertions.fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // Do nothing
        }

        try {
            buildService("INVALID_CONFIG");
            Assertions.fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // Do nothing
        }

        try {
            buildService("NEXT_INVALID_DAY_OF_WEEK");
            Assertions.fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // Do nothing
        }

        Field fieldNotificateNextDay = ReflectionUtils.findField(RewardNotificationThresholdHandlerServiceImpl.class, "notificateNextDay");
        Field fieldNotificateNextDayOfWeek = ReflectionUtils.findField(RewardNotificationThresholdHandlerServiceImpl.class, "notificateNextDayOfWeek");
        Assertions.assertNotNull(fieldNotificateNextDay);
        Assertions.assertNotNull(fieldNotificateNextDayOfWeek);
        fieldNotificateNextDay.setAccessible(true);
        fieldNotificateNextDayOfWeek.setAccessible(true);

        BaseRewardNotificationThresholdBasedHandler configurationTomorrow = buildService(NOTIFICATION_DAY_TOMORROW);
        Assertions.assertTrue(fieldNotificateNextDay.getBoolean(configurationTomorrow));
        Assertions.assertNull(fieldNotificateNextDayOfWeek.get(configurationTomorrow));

        BaseRewardNotificationThresholdBasedHandler configurationNextDayOfWeek = buildService(NOTIFICATION_DAY_NEXTDAYOFWEEK);
        Assertions.assertFalse(fieldNotificateNextDay.getBoolean(configurationNextDayOfWeek));
        Assertions.assertEquals(DayOfWeek.MONDAY, fieldNotificateNextDayOfWeek.get(configurationNextDayOfWeek));
    }

    public static String getExpectedBeneficiaryId(InitiativeRewardType rewardType, RewardTransactionDTO trx) {
        return InitiativeRewardType.REFUND.equals(rewardType)
                ? trx.getUserId()
                : trx.getMerchantId();
    }

    @ParameterizedTest
    @CsvSource({
            "false, " + NOTIFICATION_DAY_TOMORROW + ", REFUND",
            "false, " + NOTIFICATION_DAY_NEXTDAYOFWEEK + ", REFUND",
            "true, " + NOTIFICATION_DAY_TOMORROW + ", REFUND",
            "true, " + NOTIFICATION_DAY_NEXTDAYOFWEEK + ", REFUND",

            "false, " + NOTIFICATION_DAY_TOMORROW + ", DISCOUNT",
            "false, " + NOTIFICATION_DAY_NEXTDAYOFWEEK + ", DISCOUNT",
            "true, " + NOTIFICATION_DAY_TOMORROW + ", DISCOUNT",
            "true, " + NOTIFICATION_DAY_NEXTDAYOFWEEK + ", DISCOUNT",

    })
    void testHandleNewNotifyNotOverflowing(boolean isRefundTrx, String notificationDay, InitiativeRewardType rewardType) {
        // Given
        BaseRewardNotificationThresholdBasedHandler service = buildService(notificationDay);

        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(0);
        RewardNotificationRule rule = buildRule();
        rule.setInitiativeRewardType(rewardType);

        Reward reward = new Reward(BigDecimal.valueOf(isRefundTrx ? -3 : 3));

        RewardsNotification[] expectedResult = new RewardsNotification[]{null};
        long expectedProgressive = 5L;

        String expectedBeneficiaryId = getExpectedBeneficiaryId(rewardType, trx);
        String expectedNotificationId = "%s_INITIATIVEID_%d".formatted(expectedBeneficiaryId, expectedProgressive);

        Mockito.when(repositoryMock.findByBeneficiaryIdAndInitiativeIdAndNotificationDateAndStatusAndOrdinaryIdIsNull(expectedBeneficiaryId, rule.getInitiativeId(), null, RewardNotificationStatus.TO_SEND)).thenReturn(Flux.empty());
        Mockito.doAnswer(a -> {
                    expectedResult[0] = (RewardsNotification) a.callRealMethod();
                    return expectedResult[0];
                })
                .when(mapperSpy)
                .apply(Mockito.any(), Mockito.any(), Mockito.anyLong(), Mockito.any(), Mockito.any());

        Mockito.when(repositoryMock.countByBeneficiaryIdAndInitiativeIdAndOrdinaryIdIsNull(expectedBeneficiaryId, rule.getInitiativeId())).thenReturn(Mono.just(expectedProgressive - 1));

        if (isRefundTrx) {
            Mockito.when(repositoryMock.findByBeneficiaryIdAndInitiativeIdAndNotificationDateGreaterThanAndStatusAndOrdinaryIdIsNull(expectedBeneficiaryId, rule.getInitiativeId(), LocalDate.now(), RewardNotificationStatus.TO_SEND)).thenReturn(Flux.empty());
        }

        service = Mockito.spy(service);

        // When
        RewardsNotification result = service.handle(trx, rule, reward).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertSame(expectedResult[0], result);

        Assertions.assertEquals(expectedNotificationId, result.getId());
        Assertions.assertNull(result.getNotificationDate());
        Assertions.assertEquals(isRefundTrx ? -300L : 300L, result.getRewardCents());
        Assertions.assertEquals(expectedProgressive, result.getProgressive());
        Assertions.assertEquals(List.of(trx.getId()), result.getTrxIds());
        Assertions.assertEquals(getExpectedDepositType(), result.getDepositType());

        Mockito.verify(service).handle(Mockito.same(trx), Mockito.same(rule), Mockito.same(reward));
        Mockito.verify(repositoryMock).countByBeneficiaryIdAndInitiativeIdAndOrdinaryIdIsNull(expectedBeneficiaryId, rule.getInitiativeId());
        Mockito.verify(mapperSpy).apply(Mockito.eq(expectedBeneficiaryId + "_INITIATIVEID"), Mockito.isNull(), Mockito.eq(expectedProgressive), Mockito.same(trx), Mockito.same(rule));

        Mockito.verifyNoMoreInteractions(repositoryMock, mapperSpy);
    }

    @ParameterizedTest
    @CsvSource({
            "false,"+NOTIFICATION_DAY_TOMORROW+",REFUND",
            "false,"+NOTIFICATION_DAY_NEXTDAYOFWEEK+",REFUND",
            "true,"+NOTIFICATION_DAY_TOMORROW+",REFUND",
            "true,"+NOTIFICATION_DAY_NEXTDAYOFWEEK+",REFUND",

            "false,"+NOTIFICATION_DAY_TOMORROW+",DISCOUNT",
            "false,"+NOTIFICATION_DAY_NEXTDAYOFWEEK+",DISCOUNT",
            "true,"+NOTIFICATION_DAY_TOMORROW+",DISCOUNT",
            "true,"+NOTIFICATION_DAY_NEXTDAYOFWEEK+",DISCOUNT",
    })
    void testHandleNewNotifyRefundWithFutureNotification(boolean isStillOverflowing, String notificationDay, InitiativeRewardType rewardType) {
        // Given
        BaseRewardNotificationThresholdBasedHandler service = buildService(notificationDay);

        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(0);
        RewardNotificationRule rule = buildRule();
        rule.setInitiativeId("INITIATIVEID");
        rule.setEndDate(LocalDate.now());
        rule.setInitiativeRewardType(rewardType);

        Reward reward = testHandleNewNotifyRefundWithFutureNotification_buildOverFlowingReward(isStillOverflowing);

        LocalDate notificationDate = NOTIFICATION_DAY_TOMORROW.equals(notificationDay) ? TOMORROW : NEXT_MONDAY;
        long expectedProgressive = 5L;
        RewardsNotification expectedResult = RewardsNotificationFaker.mockInstance(0, rule.getInitiativeId(), notificationDate, rewardType);
        expectedResult.setProgressive(expectedProgressive);
        expectedResult.setRewardCents(600L);
        expectedResult.setNotificationDate(notificationDate);
        expectedResult.getTrxIds().add("TRXID");

        String expectedBeneficiaryId = getExpectedBeneficiaryId(rewardType, trx);

        Mockito.when(repositoryMock.findByBeneficiaryIdAndInitiativeIdAndNotificationDateAndStatusAndOrdinaryIdIsNull(expectedBeneficiaryId, rule.getInitiativeId(), null, RewardNotificationStatus.TO_SEND)).thenReturn(Flux.empty());
        Mockito.when(repositoryMock.countByBeneficiaryIdAndInitiativeIdAndOrdinaryIdIsNull(expectedBeneficiaryId, rule.getInitiativeId())).thenReturn(Mono.empty());

        Mockito.when(repositoryMock.findByBeneficiaryIdAndInitiativeIdAndNotificationDateGreaterThanAndStatusAndOrdinaryIdIsNull(expectedBeneficiaryId, rule.getInitiativeId(), LocalDate.now(), RewardNotificationStatus.TO_SEND)).thenReturn(Flux.just(expectedResult));

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
        Assertions.assertEquals(isStillOverflowing ? notificationDate : null, result.getNotificationDate());

        Mockito.verify(service).handle(Mockito.same(trx), Mockito.same(rule), Mockito.same(reward));

        Mockito.verifyNoMoreInteractions(repositoryMock, mapperSpy);
    }

    protected abstract Reward testHandleNewNotifyRefundWithFutureNotification_buildOverFlowingReward(boolean isStillOverflowing);

    protected abstract long testHandleNewNotifyRefundWithFutureNotification_expectedReward(boolean isStillOverflowing);

    @ParameterizedTest
    @EnumSource(InitiativeRewardType.class)
    void testHandleUpdateNotifyOverflowingThreshold(InitiativeRewardType rewardType) {
        // Given
        BaseRewardNotificationThresholdBasedHandler service = buildService(NOTIFICATION_DAY_TOMORROW);

        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(0);
        RewardNotificationRule rule = buildRule();
        rule.setInitiativeId("INITIATIVEID");
        rule.setEndDate(LocalDate.now().plusDays(1));
        rule.setInitiativeRewardType(rewardType);

        Reward reward = testHandleUpdateNotifyOverflowingThreshold_buildOverFlowingReward();

        LocalDate expectedNotificationDate = TOMORROW;
        long expectedProgressive = 5L;
        RewardsNotification expectedResult = RewardsNotificationFaker.mockInstance(0, rule.getInitiativeId(), expectedNotificationDate, rewardType);
        expectedResult.setProgressive(expectedProgressive);
        expectedResult.setRewardCents(200L);
        expectedResult.getTrxIds().add("TRXID");

        String expectedBeneficiaryId = getExpectedBeneficiaryId(rewardType, trx);

        Mockito.when(repositoryMock.findByBeneficiaryIdAndInitiativeIdAndNotificationDateAndStatusAndOrdinaryIdIsNull(expectedBeneficiaryId, rule.getInitiativeId(), null, RewardNotificationStatus.TO_SEND)).thenReturn(Flux.just(expectedResult));

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

    protected abstract Reward testHandleUpdateNotifyOverflowingThreshold_buildOverFlowingReward();
}
