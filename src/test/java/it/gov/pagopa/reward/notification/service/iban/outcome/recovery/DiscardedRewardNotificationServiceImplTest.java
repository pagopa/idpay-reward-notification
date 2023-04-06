package it.gov.pagopa.reward.notification.service.iban.outcome.recovery;

import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.notify.RewardNotificationBudgetExhaustedHandlerServiceImpl;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.notify.RewardNotificationTemporalHandlerServiceImpl;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.notify.RewardNotificationThresholdHandlerServiceImpl;
import it.gov.pagopa.reward.notification.service.rule.RewardNotificationRuleService;
import it.gov.pagopa.reward.notification.test.fakers.RewardNotificationRuleFaker;
import it.gov.pagopa.reward.notification.test.fakers.RewardsNotificationFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;

@ExtendWith(MockitoExtension.class)
class DiscardedRewardNotificationServiceImplTest{
    public static final LocalDate TODAY = LocalDate.now();
    public static final LocalDate TOMORROW = TODAY.plusDays(1);
    public static final LocalDate NEXT_WEEK= TODAY.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    public static final LocalDate NEXT_MONTH= TODAY.with(TemporalAdjusters.firstDayOfNextMonth());
    public static final LocalDate NEXT_QUARTER= TODAY.withDayOfMonth(1).withMonth((TODAY.get(IsoFields.QUARTER_OF_YEAR)*3)).plusMonths(1);

    @Mock
    private RewardNotificationRuleService notificationRuleServiceMock;
    @Mock
    private RewardNotificationTemporalHandlerServiceImpl temporalHandlerMock;
    @Mock
    private RewardNotificationBudgetExhaustedHandlerServiceImpl budgetExhaustedHandlerMock;
    @Mock
    private RewardNotificationThresholdHandlerServiceImpl thresholdHandlerMock;

    private DiscardedRewardNotificationService service;

    @BeforeEach
    void init() {
        service = new BaseDiscardedRewardNotificationServiceImpl(notificationRuleServiceMock, temporalHandlerMock, budgetExhaustedHandlerMock, thresholdHandlerMock);
    }

    @Test
    void testSetRemedialNotificationDateWithRule() {
        RewardNotificationRule rule = RewardNotificationRuleFaker.mockInstance(0);

        mockRuleHandler(rule);

        RewardsNotification notification = RewardsNotificationFaker.mockInstanceBuilder(0)
                .notificationDate(TODAY.minusDays(3))
                .build();
        RewardsNotification result = service.setRemedialNotificationDate(rule, notification).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(TOMORROW, result.getNotificationDate());

    }

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
}