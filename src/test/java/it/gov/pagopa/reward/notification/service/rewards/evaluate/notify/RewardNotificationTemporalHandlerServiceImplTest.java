package it.gov.pagopa.reward.notification.service.rewards.evaluate.notify;

import it.gov.pagopa.reward.notification.dto.mapper.RewardsNotificationMapper;
import it.gov.pagopa.reward.notification.dto.rule.TimeParameterDTO;
import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.enums.DepositType;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.utils.Utils;
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
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class RewardNotificationTemporalHandlerServiceImplTest {

    @Mock private RewardsNotificationRepository repositoryMock;
    @Spy private RewardsNotificationMapper mapperSpy;

    private RewardNotificationTemporalHandlerServiceImpl service;

    @BeforeEach
    void init(){
        service = new RewardNotificationTemporalHandlerServiceImpl(repositoryMock, mapperSpy);
    }

// region test calculateNotificationDate method
    private static RewardNotificationRule buildRule(TimeParameterDTO.TimeTypeEnum type) {
        RewardNotificationRule rule = new RewardNotificationRule();
        rule.setInitiativeId("INITIATIVEID");
        rule.setTimeParameter(new TimeParameterDTO());
        rule.getTimeParameter().setTimeType(type);
        return rule;
    }

    @Test
    void testCalculateNotificationDateDaily(){
        // Given
        RewardNotificationRule rule = buildRule(TimeParameterDTO.TimeTypeEnum.DAILY);

        // When
        LocalDate result = service.calculateNotificationDate(LocalDate.now(), rule);

        // Then
        Assertions.assertEquals(LocalDate.now().plusDays(1), result);
    }

    @Test
    void testCalculateNotificationDateWeekly(){
        // Given
        RewardNotificationRule rule = buildRule(TimeParameterDTO.TimeTypeEnum.WEEKLY);

        LocalDate date = LocalDate.of(2022, 10, 2); // a SUNDAY
        LocalDate expectedNotificationDate = date.plusDays(7);
        for(int i=0; i<30; i++){
            // When
            LocalDate result = service.calculateNotificationDate(date, rule);

            // Then
            Assertions.assertEquals(expectedNotificationDate.plusDays(i/7*7), result, "Invalid next notification date starting from %s".formatted(date));

            date = date.plusDays(1);
        }
    }

    @Test
    void testCalculateNotificationDateMonthly(){
        // Given
        RewardNotificationRule rule = buildRule(TimeParameterDTO.TimeTypeEnum.MONTHLY);

        LocalDate date = LocalDate.of(2022, 1, 1);
        LocalDate expectedNotificationDate = date.plusMonths(1);
        for(int i=0; i<365; i++){
            // When
            LocalDate result = service.calculateNotificationDate(date, rule);

            // Then
            Assertions.assertEquals(expectedNotificationDate.plusMonths(date.getMonthValue()-1), result, "Invalid next notification date starting from %s".formatted(date));

            date=date.plusDays(1);
        }
    }

    @Test
    void testCalculateNotificationDateQuarterly(){
        // Given
        RewardNotificationRule rule = buildRule(TimeParameterDTO.TimeTypeEnum.QUARTERLY);

        LocalDate date = LocalDate.of(2022, 1, 1);
        LocalDate expectedNotificationDate = date;
        for(int i=0; i<365; i++){
            // When
            LocalDate result = service.calculateNotificationDate(date, rule);

            // Then
            Assertions.assertEquals(expectedNotificationDate.plusMonths(((date.getMonthValue() - 1)/3+1)*3), result, "Invalid next notification date starting from %s".formatted(date));

            date=date.plusDays(1);
        }
    }

    @Test
    void testCalculateNotificationDateClosed(){
        // Given
        RewardNotificationRule rule = buildRule(TimeParameterDTO.TimeTypeEnum.CLOSED);
        rule.setEndDate(LocalDate.now().plusDays(256));

        // When
        LocalDate result = service.calculateNotificationDate(LocalDate.now(), rule);

        // Then
        Assertions.assertSame(rule.getEndDate().plusDays(1), result);
    }

    @Test
    void testCalculateNotificationDateClosedPast(){
        // Given
        RewardNotificationRule rule = buildRule(TimeParameterDTO.TimeTypeEnum.CLOSED);
        rule.setEndDate(LocalDate.now());

        // When
        LocalDate result = service.calculateNotificationDate(LocalDate.now(), rule);

        // Then
        Assertions.assertEquals(LocalDate.now().plusDays(1), result);
    }
//endregion

//region test handle method
    @Test
    void testHandleNewNotify(){
        // Given
        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(0);
        RewardNotificationRule rule = buildRule(TimeParameterDTO.TimeTypeEnum.DAILY);
        Reward reward = new Reward(BigDecimal.TEN);

        RewardsNotification[] expectedResult = new RewardsNotification[]{null};
        LocalDate expectedNotificationDate = LocalDate.now().plusDays(1);
        long expectedProgressive = 5L;

        String notificationId = "USERID0_INITIATIVEID_%s".formatted(expectedNotificationDate.format(Utils.FORMATTER_DATE));

        Mockito.when(repositoryMock.findById(notificationId)).thenReturn(Mono.empty());
        Mockito.doAnswer(a ->{
                    expectedResult[0]=(RewardsNotification)a.callRealMethod();
                    return expectedResult[0];
                })
                .when(mapperSpy)
                .apply(Mockito.any(), Mockito.any(), Mockito.anyLong(), Mockito.any(), Mockito.any());

        Mockito.when(repositoryMock.count(Mockito.any())).thenReturn(Mono.just(expectedProgressive - 1));

        service = Mockito.spy(service);

        // When
        RewardsNotification result = service.handle(trx, rule, reward).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertSame(expectedResult[0], result);

        Assertions.assertEquals(1000L, result.getRewardCents());
        Assertions.assertEquals(expectedProgressive, result.getProgressive());
        Assertions.assertEquals(List.of(trx.getId()), result.getTrxIds());
        Assertions.assertEquals(DepositType.PARTIAL, result.getDepositType());

        Mockito.verify(service).handle(Mockito.same(trx), Mockito.same(rule), Mockito.same(reward));
        Mockito.verify(service).calculateNotificationDate(Mockito.eq(LocalDate.now()), Mockito.same(rule));
        Mockito.verify(repositoryMock).count(Mockito.argThat(i->{
            RewardsNotification query = i.getProbe();
            Assertions.assertEquals(rule.getInitiativeId(), query.getInitiativeId());
            Assertions.assertEquals(trx.getUserId(), query.getUserId());
            TestUtils.checkNullFields(query, "userId", "initiativeId");
            return true;
        }));
        Mockito.verify(mapperSpy).apply(Mockito.eq(notificationId), Mockito.eq(expectedNotificationDate), Mockito.eq(expectedProgressive), Mockito.same(trx), Mockito.same(rule));

        Mockito.verifyNoMoreInteractions(repositoryMock, mapperSpy);
    }

    @Test
    void testHandleUpdateNotify(){
        // Given
        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(0);
        RewardNotificationRule rule = buildRule(TimeParameterDTO.TimeTypeEnum.DAILY);
        rule.setInitiativeId("INITIATIVEID");
        rule.setEndDate(LocalDate.now());
        Reward reward = new Reward(BigDecimal.TEN);

        LocalDate expectedNotificationDate = LocalDate.now().plusDays(1);
        long expectedProgressive = 5L;
        RewardsNotification expectedResult = RewardsNotificationFaker.mockInstance(0, rule.getInitiativeId(), expectedNotificationDate);
        expectedResult.setProgressive(expectedProgressive);
        expectedResult.setRewardCents(100L);
        expectedResult.getTrxIds().add("TRXID");

        Mockito.when(repositoryMock.findById(expectedResult.getId())).thenReturn(Mono.just(expectedResult));
        Mockito.when(repositoryMock.count(Mockito.any())).thenReturn(Mono.empty());

        service = Mockito.spy(service);

        // When
        RewardsNotification result = service.handle(trx, rule, reward).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertSame(expectedResult, result);

        Assertions.assertEquals(1100L, result.getRewardCents());
        Assertions.assertEquals(expectedProgressive, result.getProgressive());
        Assertions.assertEquals(DepositType.FINAL, result.getDepositType());
        Assertions.assertEquals(List.of("TRXID", trx.getId()), result.getTrxIds());

        Mockito.verify(service).handle(Mockito.same(trx), Mockito.same(rule), Mockito.same(reward));
        Mockito.verify(service).calculateNotificationDate(Mockito.eq(LocalDate.now()), Mockito.same(rule));

        Mockito.verifyNoMoreInteractions(repositoryMock, mapperSpy);
    }
//endregion

//region test calcDepositType method
    @Test
    void testCalcDepositTypeNoEndDateNoBudgetExhausted(){
        // Given
        RewardNotificationRule rule = buildRule(TimeParameterDTO.TimeTypeEnum.DAILY);
        rule.setInitiativeId("INITIATIVEID");
        Reward reward = new Reward(BigDecimal.TEN);

        // When
        DepositType result = service.calcDepositType(rule, reward);

        // Then
        Assertions.assertEquals(DepositType.PARTIAL, result);
    }

    @Test
    void testCalcDepositTypeFutureEndDateNoBudgetExhausted(){
        // Given
        RewardNotificationRule rule = buildRule(TimeParameterDTO.TimeTypeEnum.DAILY);
        rule.setInitiativeId("INITIATIVEID");
        rule.setEndDate(LocalDate.now().plusDays(1));
        Reward reward = new Reward(BigDecimal.TEN);

        // When
        DepositType result = service.calcDepositType(rule, reward);

        // Then
        Assertions.assertEquals(DepositType.PARTIAL, result);
    }

    @Test
    void testCalcDepositTypePastEndDateNoBudgetExhausted(){
        // Given
        RewardNotificationRule rule = buildRule(TimeParameterDTO.TimeTypeEnum.DAILY);
        rule.setInitiativeId("INITIATIVEID");
        rule.setEndDate(LocalDate.now());
        Reward reward = new Reward(BigDecimal.TEN);

        // When
        DepositType result = service.calcDepositType(rule, reward);

        // Then
        Assertions.assertEquals(DepositType.FINAL, result);
    }

    @Test
    void testCalcDepositTypeBudgetExhausted(){
        // Given
        RewardNotificationRule rule = buildRule(TimeParameterDTO.TimeTypeEnum.DAILY);
        rule.setInitiativeId("INITIATIVEID");
        Reward reward = new Reward(BigDecimal.TEN);
        reward.getCounters().setExhaustedBudget(true);

        // When
        DepositType result = service.calcDepositType(rule, reward);

        // Then
        Assertions.assertEquals(DepositType.FINAL, result);
    }
//endregion
}
