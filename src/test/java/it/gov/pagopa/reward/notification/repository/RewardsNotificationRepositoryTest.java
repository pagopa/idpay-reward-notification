package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.service.utils.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

class RewardsNotificationRepositoryTest extends BaseIntegrationTest {

    public static final LocalDate TODAY = LocalDate.now();
    public static final LocalDate TOMORROW = TODAY.plusDays(1);

    public static final String TEST_USERID = "TEST_USERID";
    public static final String TEST_USERID2 = "TEST_USERID2";
    public static final String TEST_INITIATIVEID = "TEST_INITIATIVEID";
    public static final String TEST_INITIATIVEID2 = "TEST_INITIATIVEID2";

    @Autowired
    private RewardsNotificationRepository rewardsNotificationRepository;

    @BeforeEach
    void prepareTestData() {
        rewardsNotificationRepository.saveAll(
                Stream.of(
                        // useCases actual user and searched initiative
                        buildUseCase(TEST_USERID, TEST_INITIATIVEID),
                        // useCases actual user and other initiative
                        buildUseCase(TEST_USERID, TEST_INITIATIVEID2),
                        // useCases other user and searched initiative
                        buildUseCase(TEST_USERID2, TEST_INITIATIVEID)
                ).flatMap(List::stream).toList()
        ).collectList().block();
    }

    @AfterEach
    void cleanData(){
        deleteByUserId(TEST_USERID);
        deleteByUserId(TEST_USERID2);
    }

    private void deleteByUserId(String userId) {
        rewardsNotificationRepository.findAll(Example.of(RewardsNotification.builder().userId(userId).build()))
                .flatMap(rewardsNotificationRepository::delete)
                .collectList()
                .block();
    }

    private List<RewardsNotification> buildUseCase(String userId, String initiativeId) {
        return List.of(
                // use case past notification
                RewardsNotification.builder()
                        .id("%s_%s_%s".formatted(userId, initiativeId, Utils.FORMATTER_DATE.format(TODAY)))
                        .userId(userId)
                        .initiativeId(initiativeId)
                        .notificationDate(TODAY)
                        .build(),

                // use case future notification
                RewardsNotification.builder()
                        .id("%s_%s_%s".formatted(userId, initiativeId, Utils.FORMATTER_DATE.format(TOMORROW)))
                        .userId(userId)
                        .initiativeId(initiativeId)
                        .notificationDate(TOMORROW)
                        .build(),

                // use case not configured
                RewardsNotification.builder()
                        .id("%s_%s".formatted(userId, initiativeId))
                        .userId(userId)
                        .initiativeId(initiativeId)
                        .build()
        );
    }

    @Test
    void testFindByUserIdAndInititiaveIdAndNotificationDate() {
        String userId = TEST_USERID;
        String initiativeId = TEST_INITIATIVEID;

        List<RewardsNotification> result = rewardsNotificationRepository.findByUserIdAndInitiativeIdAndNotificationDate(userId, initiativeId, null).collectList().block();

        checkResult(result, "%s_%s".formatted(userId, initiativeId));

        result = rewardsNotificationRepository.findByUserIdAndInitiativeIdAndNotificationDate(userId, initiativeId, TODAY).collectList().block();

        checkResult(result, "%s_%s_%s".formatted(userId, initiativeId, Utils.FORMATTER_DATE.format(TODAY)));
    }

    @Test
    void testFindByUserIdAndInitiativeIdAndNotificationDateGreaterThan() {
        String userId = TEST_USERID;
        String initiativeId = TEST_INITIATIVEID;

        List<RewardsNotification> result = rewardsNotificationRepository.findByUserIdAndInitiativeIdAndNotificationDateGreaterThan(userId, initiativeId, TODAY).collectList().block();

        checkResult(result, "%s_%s_%s".formatted(userId, initiativeId, Utils.FORMATTER_DATE.format(TOMORROW)));
    }

    private static void checkResult(List<RewardsNotification> result, String expectedId) {
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(
                expectedId,
                result.get(0).getId()
        );
    }
}
