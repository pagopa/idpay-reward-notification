package it.gov.pagopa.reward.notification.dto.mapper;

import it.gov.pagopa.reward.notification.dto.rewards.RewardFeedbackDTO;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.test.fakers.RewardsNotificationFaker;
import it.gov.pagopa.reward.notification.test.utils.TestUtils;
import it.gov.pagopa.reward.notification.utils.Utils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

class RewardFeedbackMapperTest {
    private final RewardFeedbackMapper mapper = new RewardFeedbackMapper();

    @Test
    void testStatusOK() {
        // Given
        RewardsNotification notification = RewardsNotificationFaker.mockInstance(1);
        notification.setStatus(RewardNotificationStatus.COMPLETED_OK);
        notification.setFeedbackDate(LocalDateTime.now());
        notification.setIban("IBAN");
        notification.setFeedbackElaborationDate(LocalDateTime.now());
        notification.setExecutionDate(LocalDate.now().plusDays(2));
        notification.setCro("CRO");

        long deltaRewardCents = 0L;

        // When
        RewardFeedbackDTO result = mapper.apply(notification, deltaRewardCents);

        // Then
        Assertions.assertNotNull(result);

        checkCommonFields(notification, deltaRewardCents, RewardFeedbackMapper.REWARD_NOTIFICATION_FEEDBACK_STATUS_ACCEPTED, result);

        TestUtils.checkNotNullFields(result, "rejectionCode", "rejectionReason");
    }

    @Test
    void testStatusKO() {

        testKo(RewardNotificationStatus.COMPLETED_KO);
    }

    @Test
    void testStatusError() {
        testKo(RewardNotificationStatus.ERROR);
    }

    private void testKo(RewardNotificationStatus errorStatus) {
        // Given
        RewardsNotification notification = RewardsNotificationFaker.mockInstance(0);
        notification.setStatus(errorStatus);
        notification.setResultCode("ERROR_CODE");
        notification.setRejectionReason("ERROR_REASON");
        notification.setIban("IBAN");
        notification.setExecutionDate(LocalDate.now());
        notification.setFeedbackElaborationDate(LocalDateTime.now());
        notification.setFeedbackHistory(List.of(new RewardsNotification.RewardNotificationHistory()));

        long deltaRewardCents = 0L;

        // When
        RewardFeedbackDTO result = mapper.apply(notification, deltaRewardCents);

        // Then
        Assertions.assertNotNull(result);

        checkCommonFields(notification, deltaRewardCents, RewardFeedbackMapper.REWARD_NOTIFICATION_FEEDBACK_STATUS_REJECTED, result);

        TestUtils.checkNotNullFields(result, "feedbackDate", "executionDate", "cro");
    }

    @Test
    void testInvalidStatus() {
        Set<RewardNotificationStatus> handledStatuses = Set.of(
                RewardNotificationStatus.COMPLETED_OK,
                RewardNotificationStatus.COMPLETED_KO,
                RewardNotificationStatus.ERROR
        );
        Arrays.stream(RewardNotificationStatus.values())
                .filter(s-> !handledStatuses.contains(s))
                .forEach(this::testInvalidStatus);
    }

    void testInvalidStatus(RewardNotificationStatus invalidStatus) {
        // Given
        RewardsNotification notification = RewardsNotificationFaker.mockInstance(0);
        notification.setStatus(invalidStatus);

        long deltaRewardCents = 0L;

        try {
            // When
            mapper.apply(notification, deltaRewardCents);
            Assertions.fail("Expected exception");
        } catch (IllegalArgumentException e){
            Assertions.assertEquals("Invalid notification status %s when sending %s".formatted(notification.getStatus(), notification.getId())
                    , e.getMessage());
        }
    }

    private void checkCommonFields(RewardsNotification notification, long deltaRewardCents, String expectedStatus, RewardFeedbackDTO result) {
        Assertions.assertEquals(notification.getId(), result.getRewardNotificationId());
        Assertions.assertEquals(notification.getInitiativeId(), result.getInitiativeId());
        Assertions.assertEquals(notification.getBeneficiaryId(), result.getUserId());
        Assertions.assertEquals(notification.getOrganizationId(), result.getOrganizationId());
        Assertions.assertEquals(expectedStatus, result.getStatus());
        Assertions.assertEquals(RewardFeedbackMapper.REWARD_NOTIFICATION_FEEDBACK_STATUS_ACCEPTED.equals(expectedStatus) ? null : notification.getResultCode(), result.getRejectionCode());
        Assertions.assertEquals(notification.getRejectionReason(), result.getRejectionReason());
        Assertions.assertEquals(notification.getRewardCents(), result.getEffectiveRewardCents());
        Assertions.assertEquals(deltaRewardCents, result.getRewardCents());
        Assertions.assertEquals(notification.getFeedbackDate(), result.getFeedbackDate());
        Assertions.assertEquals(notification.getFeedbackHistory().size(), result.getFeedbackProgressive());
        Assertions.assertEquals(notification.getExecutionDate(), result.getExecutionDate());
        Assertions.assertEquals(notification.getCro(), result.getCro());
        Assertions.assertEquals(notification.getIban(), result.getIban());
        Assertions.assertEquals(notification.getStatus(), result.getRewardStatus());
        Assertions.assertEquals(Utils.RefundType.ORDINARY, result.getRefundType());
        Assertions.assertEquals(notification.getStartDepositDate(), result.getStartDate());
        Assertions.assertEquals(notification.getNotificationDate(), result.getEndDate());
        Assertions.assertEquals(notification.getExecutionDate(), result.getTransferDate());
        Assertions.assertEquals(notification.getFeedbackElaborationDate().toLocalDate(), result.getUserNotificationDate());
    }
}
