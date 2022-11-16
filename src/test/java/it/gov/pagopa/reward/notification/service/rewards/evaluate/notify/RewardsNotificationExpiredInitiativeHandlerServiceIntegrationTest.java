package it.gov.pagopa.reward.notification.service.rewards.evaluate.notify;

import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardNotificationRuleRepository;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.test.fakers.RewardNotificationRuleFaker;
import it.gov.pagopa.reward.notification.test.fakers.RewardsNotificationFaker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.util.List;


class RewardsNotificationExpiredInitiativeHandlerServiceIntegrationTest extends BaseIntegrationTest {

    public static final LocalDate TODAY = LocalDate.now();

    private RewardNotificationRule expiredInitiative;
    private RewardNotificationRule validInitiative;

    private RewardsNotification expiredNotification1;
    private RewardsNotification expiredNotification2;
    private RewardsNotification validNotification1;
    private RewardsNotification validNotification2;

    @Autowired
    private RewardNotificationRuleRepository ruleRepository;
    @Autowired
    private RewardsNotificationRepository notificationRepository;
    @Value("${app.rewards-notification.expired-initiatives.day-before}")
    private int dayBefore;

    private RewardsNotificationExpiredInitiativeHandlerService service;

    private void storeTestData() {
        // initiatives
        expiredInitiative = ruleRepository.save(
                RewardNotificationRuleFaker.mockInstanceBuilder(1)
                        .initiativeId("INITIATIVE1")
                        .endDate(TODAY.minusDays(5))
                        .build()
        ).block();
        validInitiative = ruleRepository.save(
                RewardNotificationRuleFaker.mockInstanceBuilder(2)
                        .initiativeId("INITIATIVE2")
                        .endDate(TODAY.plusDays(30))
                        .build()
        ).block();

        // notifications
        expiredNotification1 = notificationRepository.save(
                RewardsNotificationFaker.mockInstanceBuilder(1)
                        .initiativeId("INITIATIVE1")
                        .notificationDate(TODAY.minusDays(6))
                        .build()
        ).block();
        expiredNotification2 = notificationRepository.save(
                RewardsNotificationFaker.mockInstanceBuilder(2)
                        .initiativeId("INITIATIVE1")
                        .notificationDate(null)
                        .build()
        ).block();
        validNotification1 = notificationRepository.save(
                RewardsNotificationFaker.mockInstanceBuilder(3)
                        .initiativeId("INITIATIVE2")
                        .notificationDate(TODAY)
                        .build()
        ).block();
        validNotification2 = notificationRepository.save(
                RewardsNotificationFaker.mockInstanceBuilder(4)
                        .initiativeId("INITIATIVE2")
                        .notificationDate(null)
                        .build()
        ).block();
    }

    @BeforeEach
    void init() {
        service = new RewardsNotificationExpiredInitiativeHandlerServiceImpl(ruleRepository, notificationRepository, dayBefore);
        storeTestData();
    }

    @AfterEach
    void clearData() {
        ruleRepository.deleteAll().block();
        notificationRepository.deleteAll().block();
    }

    @Test
    void testHandle() {

        // When
        List<RewardsNotification> result = service.handle().collectList().block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        checkHandleResult(expiredNotification2, result.get(0));
    }

    void checkHandleResult(RewardsNotification expected, RewardsNotification actual) {
        Assertions.assertEquals(expected.getId(), actual.getId());
        Assertions.assertEquals(expected.getInitiativeId(), actual.getInitiativeId());

        Assertions.assertNull(expected.getNotificationDate());
        Assertions.assertNotNull(actual.getNotificationDate());
        Assertions.assertEquals(TODAY.plusDays(1), actual.getNotificationDate());
    }

}
