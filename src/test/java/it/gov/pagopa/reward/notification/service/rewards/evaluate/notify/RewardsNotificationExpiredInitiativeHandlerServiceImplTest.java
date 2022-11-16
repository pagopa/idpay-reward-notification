package it.gov.pagopa.reward.notification.service.rewards.evaluate.notify;

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
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class RewardsNotificationExpiredInitiativeHandlerServiceImplTest {

    private final int dayBefore = 7;
    @Mock private RewardsNotificationRepository rewardsNotificationRepository;
    @Mock private RewardNotificationRuleRepository rewardNotificationRuleRepository;

    private RewardsNotificationExpiredInitiativeHandlerServiceImpl service;

    @BeforeEach
    void init() {
        service = Mockito.spy(new RewardsNotificationExpiredInitiativeHandlerServiceImpl(rewardNotificationRuleRepository, rewardsNotificationRepository, dayBefore));
    }

    @Test
    void testSchedule() {
        Mockito.doReturn(Flux.empty()).when(service).handle();

        service.schedule();

        Mockito.verify(service).handle();
    }

    @Test
    void testHandle() {
        // Given
        LocalDate today = LocalDate.now();

        RewardNotificationRule expectedRule = RewardNotificationRuleFaker.mockInstance(1);
        RewardsNotification expectedNotification = RewardsNotificationFaker.mockInstance(1);

        Mockito.when(
                rewardNotificationRuleRepository
                        .findByAccumulatedAmountNotNullAndEndDateBetween(today.minusDays(dayBefore), today)
        ).thenReturn(Flux.just(expectedRule));

        Mockito.when(rewardsNotificationRepository.findByInitiativeIdAndNotificationDate(expectedRule.getInitiativeId(), null))
                .thenReturn(Flux.just(expectedNotification));

        // When
        List<RewardsNotification> result = service.handle().collectList().block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(today.plusDays(1), result.get(0).getNotificationDate());

    }
}