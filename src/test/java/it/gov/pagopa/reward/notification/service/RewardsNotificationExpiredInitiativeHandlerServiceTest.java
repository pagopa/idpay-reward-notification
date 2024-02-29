package it.gov.pagopa.reward.notification.service;

import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardNotificationRuleRepository;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.test.fakers.RewardNotificationRuleFaker;
import it.gov.pagopa.reward.notification.test.fakers.RewardsNotificationFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class RewardsNotificationExpiredInitiativeHandlerServiceTest {

    public static final LocalDate TODAY = LocalDate.now();

    @Mock
    private RewardNotificationRuleRepository ruleRepository;
    @Mock
    private RewardsNotificationRepository notificationRepository;
    @Value("${app.rewards-notification.expired-initiatives.day-before:30}")
    private int dayBefore;

    private RewardsNotificationExpiredInitiativeHandlerService service;

    @BeforeEach
    void init() {
        service = new RewardsNotificationExpiredInitiativeHandlerServiceImpl(ruleRepository, notificationRepository, dayBefore);
    }

    @Test
    void testHandle() {
        // Given
        RewardNotificationRule rewardNotificationRule = RewardNotificationRuleFaker.mockInstance(1);
        rewardNotificationRule.setInitiativeId("INITIATIVE1");
        rewardNotificationRule.setEndDate(TODAY.plusDays(30));

        RewardsNotification expectedNotification = RewardsNotificationFaker.mockInstance(1);

        Mockito.when(ruleRepository.findByAccumulatedAmountNotNullAndEndDateBetween(Mockito.any(),Mockito.any())).thenReturn(Flux.just(rewardNotificationRule));
        Mockito.when(notificationRepository.findByInitiativeIdAndNotificationDate("INITIATIVE1", null)).thenReturn(Flux.just(expectedNotification));
        Mockito.when(notificationRepository.save(expectedNotification)).thenReturn(Mono.just(expectedNotification));
        // When
        List<RewardsNotification> result = service.handle().collectList().block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
    }

}
