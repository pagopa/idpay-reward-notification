package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.common.mongo.MongoTest;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.utils.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
@MongoTest
class RewardsNotificationRepositoryTest {

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
    void cleanData() {
        deleteByUserId(TEST_USERID);
        deleteByUserId(TEST_USERID2);
    }

    private void deleteByUserId(String userId) {
        rewardsNotificationRepository.findAll(Example.of(RewardsNotification.builder().beneficiaryId(userId).build()))
                .flatMap(rewardsNotificationRepository::delete)
                .collectList()
                .block();
    }

    private List<RewardsNotification> buildUseCase(String userId, String initiativeId) {
        return List.of(
                // use case past notification already exported
                RewardsNotification.builder()
                        .id("%s_%s_%s".formatted(userId, initiativeId, Utils.FORMATTER_DATE.format(TODAY)))
                        .beneficiaryId(userId)
                        .initiativeId(initiativeId)
                        .notificationDate(TODAY)
                        .status(RewardNotificationStatus.EXPORTED)
                        .build(),

                // use case past notification
                RewardsNotification.builder()
                        .id("%s_%s_%s_1".formatted(userId, initiativeId, Utils.FORMATTER_DATE.format(TODAY)))
                        .beneficiaryId(userId)
                        .initiativeId(initiativeId)
                        .notificationDate(TODAY)
                        .status(RewardNotificationStatus.TO_SEND)
                        .build(),

                // use case future notification already exported
                RewardsNotification.builder()
                        .id("%s_%s_%s".formatted(userId, initiativeId, Utils.FORMATTER_DATE.format(TOMORROW)))
                        .beneficiaryId(userId)
                        .initiativeId(initiativeId)
                        .notificationDate(TOMORROW)
                        .status(RewardNotificationStatus.COMPLETED_OK)
                        .build(),

                // use case future notification
                RewardsNotification.builder()
                        .id("%s_%s_%s_1".formatted(userId, initiativeId, Utils.FORMATTER_DATE.format(TOMORROW)))
                        .beneficiaryId(userId)
                        .initiativeId(initiativeId)
                        .notificationDate(TOMORROW)
                        .status(RewardNotificationStatus.TO_SEND)
                        .build(),

                // use case not configured
                RewardsNotification.builder()
                        .id("%s_%s".formatted(userId, initiativeId))
                        .beneficiaryId(userId)
                        .initiativeId(initiativeId)
                        .status(RewardNotificationStatus.TO_SEND)
                        .build(),

                // use case recovery notification
                RewardsNotification.builder()
                        .id("%s_%s_%s-recovery-1".formatted(userId, initiativeId, Utils.FORMATTER_DATE.format(TODAY)))
                        .ordinaryId("%s_%s_%s".formatted(userId, initiativeId, Utils.FORMATTER_DATE.format(TODAY)))
                        .beneficiaryId(userId)
                        .initiativeId(initiativeId)
                        .notificationDate(TODAY)
                        .status(RewardNotificationStatus.TO_SEND)
                        .build()
        );
    }

    @Test
    void testFindByUserIdAndInititiaveIdAndNotificationDate() {
        String userId = TEST_USERID;
        String initiativeId = TEST_INITIATIVEID;

        List<RewardsNotification> result = rewardsNotificationRepository.findByBeneficiaryIdAndInitiativeIdAndNotificationDateAndStatusAndOrdinaryIdIsNull(userId, initiativeId, null ,RewardNotificationStatus.TO_SEND).collectList().block();

        checkResult(result, "%s_%s".formatted(userId, initiativeId));

        result = rewardsNotificationRepository.findByBeneficiaryIdAndInitiativeIdAndNotificationDateAndStatusAndOrdinaryIdIsNull(userId, initiativeId, TODAY,RewardNotificationStatus.TO_SEND).collectList().block();

        checkResult(result, "%s_%s_%s_1".formatted(userId, initiativeId, Utils.FORMATTER_DATE.format(TODAY)));
    }

    @Test
    void testFindByUserIdAndInitiativeIdAndNotificationDateGreaterThan() {
        String userId = TEST_USERID;
        String initiativeId = TEST_INITIATIVEID;

        List<RewardsNotification> result = rewardsNotificationRepository.findByBeneficiaryIdAndInitiativeIdAndNotificationDateGreaterThanAndStatusAndOrdinaryIdIsNull(userId, initiativeId, TODAY,RewardNotificationStatus.TO_SEND).collectList().block();

        checkResult(result, "%s_%s_%s_1".formatted(userId, initiativeId, Utils.FORMATTER_DATE.format(TOMORROW)));
    }

    private static void checkResult(List<RewardsNotification> result, String expectedId) {
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(
                expectedId,
                result.get(0).getId()
        );
    }

    @Test
    void testUpdateExportStatus() {
        // Given
        String id = "%s_%s_%s".formatted(TEST_USERID, TEST_INITIATIVEID, Utils.FORMATTER_DATE.format(TODAY));

        // When
        String updatedRewardNotificationId = rewardsNotificationRepository.updateExportStatus(id, "IBAN", "CHECKIBANRESULT", "EXPORTID").block();
        Assertions.assertNotNull(updatedRewardNotificationId);

        // Then
        RewardsNotification result = rewardsNotificationRepository.findById(id).block();
        Assertions.assertNotNull(result);

        Assertions.assertEquals("IBAN", result.getIban());
        Assertions.assertEquals("CHECKIBANRESULT", result.getCheckIbanResult());
        Assertions.assertEquals("EXPORTID", result.getExportId());
        Assertions.assertEquals(RewardNotificationStatus.EXPORTED, result.getStatus());
    }
}
