package it.gov.pagopa.reward.notification.service;

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
import java.util.ArrayList;
import java.util.List;


class RewardsNotificationExpiredInitiativeHandlerServiceIntegrationTest extends BaseIntegrationTest {

    public static final LocalDate TODAY = LocalDate.now();

    private final List<RewardNotificationRule> initiativesTestData = new ArrayList<>();
    private final List<RewardsNotification> notificationsTestData = new ArrayList<>();

    @Autowired
    private RewardNotificationRuleRepository ruleRepository;
    @Autowired
    private RewardsNotificationRepository notificationRepository;
    @Value("${app.rewards-notification.expired-initiatives.day-before}")
    private int dayBefore;

    private RewardsNotificationExpiredInitiativeHandlerService service;

    private void storeTestData() {
        // initiatives
        initiativesTestData.add(ruleRepository.save(
                RewardNotificationRuleFaker.mockInstanceBuilder(1)
                        .initiativeId("INITIATIVE1")
                        .endDate(TODAY.minusDays(5))
                        .build()
        ).block());
        initiativesTestData.add(ruleRepository.save(
                RewardNotificationRuleFaker.mockInstanceBuilder(2)
                        .initiativeId("INITIATIVE2")
                        .endDate(TODAY.plusDays(30))
                        .build()
        ).block());
        initiativesTestData.add(ruleRepository.save(
                RewardNotificationRuleFaker.mockInstanceBuilder(3)
                        .initiativeId("INITIATIVE3")
                        .endDate(TODAY.minusDays(5))
                        .accumulatedAmount(null)
                        .build()
        ).block());
        initiativesTestData.add(ruleRepository.save(
                RewardNotificationRuleFaker.mockInstanceBuilder(3)
                        .initiativeId("INITIATIVE4")
                        .endDate(null)
                        .build()
        ).block());

        // notifications
        notificationsTestData.add(notificationRepository.save(
                RewardsNotificationFaker.mockInstanceBuilder(1)
                        .initiativeId("INITIATIVE1")
                        .notificationDate(TODAY.minusDays(6))
                        .build()
        ).block());
        notificationsTestData.add(notificationRepository.save(
                RewardsNotificationFaker.mockInstanceBuilder(2)
                        .initiativeId("INITIATIVE1")
                        .notificationDate(null)
                        .build()
        ).block());
        notificationsTestData.add(notificationRepository.save(
                RewardsNotificationFaker.mockInstanceBuilder(3)
                        .initiativeId("INITIATIVE2")
                        .notificationDate(TODAY)
                        .build()
        ).block());
        notificationsTestData.add(notificationRepository.save(
                RewardsNotificationFaker.mockInstanceBuilder(4)
                        .initiativeId("INITIATIVE2")
                        .notificationDate(null)
                        .build()
        ).block());
        notificationsTestData.add(notificationRepository.save(
                RewardsNotificationFaker.mockInstanceBuilder(5)
                        .initiativeId("INITIATIVE3")
                        .notificationDate(null)
                        .build()
        ).block());
        notificationsTestData.add(notificationRepository.save(
                RewardsNotificationFaker.mockInstanceBuilder(6)
                        .initiativeId("INITIATIVE4")
                        .notificationDate(TODAY)
                        .build()
        ).block());
        notificationsTestData.add(notificationRepository.save(
                RewardsNotificationFaker.mockInstanceBuilder(7)
                        .initiativeId("INITIATIVE4")
                        .notificationDate(null)
                        .build()
        ).block());

    }

    @BeforeEach
    void init() {
        service = new RewardsNotificationExpiredInitiativeHandlerServiceImpl(ruleRepository, notificationRepository, dayBefore);
        storeTestData();
    }

    @AfterEach
    void clearData() {
        ruleRepository.deleteAll(initiativesTestData).block();
        notificationRepository.deleteAll(notificationsTestData).block();
    }

    @Test
    void testHandle() {
        // Given
        RewardsNotification expectedNotification = notificationsTestData.get(1);

        // When
        List<RewardsNotification> result = service.handle().collectList().block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        checkHandleResult(expectedNotification, result.get(0));
        checkResultFromRepo(result);
    }

    void checkResultFromRepo(List<RewardsNotification> result) {
        List<String> resultIds = new ArrayList<>();

        for(RewardsNotification n : result) {
            resultIds.add(n.getId());
            RewardsNotification entry = notificationRepository.findById(n.getId()).block();
            Assertions.assertNotNull(entry);
            Assertions.assertEquals(n, entry);
        }

        for (RewardsNotification n : notificationsTestData) {
            if (!resultIds.contains(n.getId())) {
                RewardsNotification entry = notificationRepository.findById(n.getId()).block();
                Assertions.assertNotNull(entry);
                Assertions.assertEquals(n, entry);
            }
        }
    }

    void checkHandleResult(RewardsNotification expected, RewardsNotification actual) {
        Assertions.assertEquals(expected.getId(), actual.getId());
        Assertions.assertEquals(expected.getInitiativeId(), actual.getInitiativeId());

        Assertions.assertNull(expected.getNotificationDate());
        Assertions.assertNotNull(actual.getNotificationDate());
        Assertions.assertEquals(TODAY.plusDays(1), actual.getNotificationDate());
    }

}
