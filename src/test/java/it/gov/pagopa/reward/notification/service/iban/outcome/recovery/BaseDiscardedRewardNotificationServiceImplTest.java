package it.gov.pagopa.reward.notification.service.iban.outcome.recovery;

import it.gov.pagopa.reward.notification.dto.rule.AccumulatedAmountDTO;
import it.gov.pagopa.reward.notification.dto.rule.TimeParameterDTO;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardIban;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.RewardsNotificationDateReschedulerService;
import it.gov.pagopa.reward.notification.service.RewardsNotificationDateReschedulerServiceImpl;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.notify.RewardNotificationBudgetExhaustedHandlerServiceImpl;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.notify.RewardNotificationTemporalHandlerServiceImpl;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.notify.RewardNotificationThresholdHandlerServiceImpl;
import it.gov.pagopa.reward.notification.service.rule.RewardNotificationRuleService;
import it.gov.pagopa.reward.notification.test.fakers.RewardNotificationRuleFaker;
import it.gov.pagopa.reward.notification.test.fakers.RewardsNotificationFaker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.List;

abstract class BaseDiscardedRewardNotificationServiceImplTest {

    public static final LocalDate TODAY = LocalDate.now();
    public static final LocalDate YESTERDAY = TODAY.minusDays(1);
    public static final LocalDate TOMORROW = TODAY.plusDays(1);
    public static final LocalDate NEXT_WEEK= TODAY.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    public static final LocalDate NEXT_MONTH= TODAY.with(TemporalAdjusters.firstDayOfNextMonth());
    public static final LocalDate NEXT_QUARTER= TODAY.withDayOfMonth(1).withMonth((TODAY.get(IsoFields.QUARTER_OF_YEAR)*3)).plusMonths(1);

    @Mock protected RewardsNotificationRepository rewardsNotificationRepositoryMock;
    @Mock protected RewardNotificationRuleService notificationRuleServiceMock;
    @Mock protected RewardNotificationTemporalHandlerServiceImpl temporalHandlerMock;
    @Mock protected RewardNotificationBudgetExhaustedHandlerServiceImpl budgetExhaustedHandlerMock;
    @Mock protected RewardNotificationThresholdHandlerServiceImpl thresholdHandlerMock;

    protected RewardsNotificationDateReschedulerService buildReschedulerService() {
        return new RewardsNotificationDateReschedulerServiceImpl(
                notificationRuleServiceMock,
                temporalHandlerMock,
                budgetExhaustedHandlerMock,
                thresholdHandlerMock);
    }
    @AfterEach
    void verifyNoMoreInvocations(){
        Mockito.verifyNoMoreInteractions(
                rewardsNotificationRepositoryMock,
                notificationRuleServiceMock,
                temporalHandlerMock,
                budgetExhaustedHandlerMock,
                thresholdHandlerMock);
    }

    @Test
    void testNoDiscard(){
        //Given
        RewardIban rewardIban = new RewardIban();
        rewardIban.setUserId("USERID");
        rewardIban.setInitiativeId("INITIATIVEID");

        getRewardNotificationRepoFindDiscardedMockWhen(rewardIban)
                .thenReturn(Flux.empty());

        // When
        List<RewardsNotification> result = invokeService(rewardIban).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(Collections.emptyList(), result);
    }

    @Test
    void testWhenSaveError(){
        //Given
        RewardIban rewardIban = new RewardIban();
        rewardIban.setUserId("USERID");
        rewardIban.setInitiativeId("INITIATIVEID");

        RewardNotificationRule rule = RewardNotificationRuleFaker.mockInstance(0);

        Mockito.when(notificationRuleServiceMock.findById("INITIATIVEID")).thenReturn(Mono.just(rule));

        mockRuleHandler(rule);

        Mockito.when(rewardsNotificationRepositoryMock.save(Mockito.any())).thenReturn(Mono.error(new RuntimeException("DUMMY ERROR WHEN SAVING")));

        getRewardNotificationRepoFindDiscardedMockWhen(rewardIban)
                .thenReturn(Flux.just(RewardsNotificationFaker.mockInstance(0)));

        // When
        List<RewardsNotification> result = invokeService(rewardIban).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(Collections.emptyList(), result);

        Mockito.verify(temporalHandlerMock).calculateNotificationDate(TODAY, rule);
    }

    protected abstract OngoingStubbing<Flux<RewardsNotification>> getRewardNotificationRepoFindDiscardedMockWhen(RewardIban rewardIban);

    protected abstract Mono<List<RewardsNotification>> invokeService(RewardIban rewardIban);

    @Test
    void testExpiredInitiative(){
        RewardNotificationRule rule = RewardNotificationRuleFaker.mockInstance(1);
        rule.setEndDate(YESTERDAY);

        List<RewardsNotification> remedials = test(rule, NEXT_WEEK);

        Mockito.verify(budgetExhaustedHandlerMock, Mockito.times(remedials.size())).calculateNotificationDate();
    }

    @Test
    void testTemporalInitiative(){
        RewardNotificationRule rule = RewardNotificationRuleFaker.mockInstance(1);
        rule.setEndDate(TODAY);
        rule.setTimeParameter(TimeParameterDTO.builder().timeType(TimeParameterDTO.TimeTypeEnum.DAILY).build());

        List<RewardsNotification> remedials = test(rule, TOMORROW);

        Mockito.verify(temporalHandlerMock, Mockito.times(remedials.size())).calculateNotificationDate(TODAY, rule);
    }

    @Test
    void testThresholdInitiative(){
        RewardNotificationRule rule = RewardNotificationRuleFaker.mockInstance(1);
        rule.setEndDate(TODAY);
        rule.setTimeParameter(null);
        rule.setAccumulatedAmount(AccumulatedAmountDTO.builder().accumulatedType(AccumulatedAmountDTO.AccumulatedTypeEnum.THRESHOLD_REACHED).refundThresholdCents(1000L).build());

        List<RewardsNotification> remedials = test(rule, NEXT_WEEK);

        Mockito.verify(thresholdHandlerMock, Mockito.times(remedials.size())).calculateNotificationDate();
    }

    @Test
    void testBudgetExhaustedInitiative(){
        RewardNotificationRule rule = RewardNotificationRuleFaker.mockInstance(1);
        rule.setEndDate(TODAY);
        rule.setTimeParameter(null);
        rule.setAccumulatedAmount(AccumulatedAmountDTO.builder().accumulatedType(AccumulatedAmountDTO.AccumulatedTypeEnum.BUDGET_EXHAUSTED).build());

        List<RewardsNotification> remedials = test(rule, NEXT_WEEK);

        Mockito.verify(budgetExhaustedHandlerMock, Mockito.times(remedials.size())).calculateNotificationDate();
    }

    private List<RewardsNotification> test(RewardNotificationRule rule, LocalDate expectedNotificationDate) {
        // Given
        RewardIban rewardIban = new RewardIban();
        rewardIban.setUserId("USERID");
        rewardIban.setInitiativeId(rule.getInitiativeId());

        mockRuleHandler(rule);

        List<RewardsNotification> remedials = test(rewardIban, rule, expectedNotificationDate);
        remedials.forEach(r -> verifyRemedialStatusFields(r, expectedNotificationDate));

        return remedials;
    }

    protected abstract List<RewardsNotification> test(RewardIban rewardIban, RewardNotificationRule rule, LocalDate expectedNotificationDate);

    private void mockRuleHandler(RewardNotificationRule rule) {
        Mockito.lenient()
                .when(temporalHandlerMock.calculateNotificationDate(TODAY, rule))
                .thenAnswer(r -> {
                    if(rule.getTimeParameter()!=null){
                        return switch (rule.getTimeParameter().getTimeType()){
                            case CLOSED -> rule.getEndDate().plusDays(1);
                            case DAILY -> TOMORROW;
                            case WEEKLY -> NEXT_WEEK;
                            case MONTHLY -> NEXT_MONTH;
                            case QUARTERLY -> NEXT_QUARTER;
                        };
                    } else {
                        return null;
                    }
                });

        Mockito.lenient()
                .when(budgetExhaustedHandlerMock.calculateNotificationDate())
                .thenReturn(NEXT_WEEK);

        Mockito.lenient()
                .when(thresholdHandlerMock.calculateNotificationDate())
                .thenReturn(NEXT_WEEK);
    }

    protected static void verifyRemedialStatusFields(RewardsNotification result, LocalDate expectedNotificationDate) {
        Assertions.assertEquals(RewardNotificationStatus.TO_SEND, result.getStatus());
        Assertions.assertNull(result.getRejectionReason());
        Assertions.assertNull(result.getResultCode());
        Assertions.assertNull(result.getFeedbackDate());
        Assertions.assertEquals(Collections.emptyList(), result.getFeedbackHistory());
        Assertions.assertEquals(expectedNotificationDate, result.getNotificationDate());
    }

}
