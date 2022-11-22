package it.gov.pagopa.reward.notification.dto.mapper;

import it.gov.pagopa.reward.notification.dto.rewards.RewardFeedbackDTO;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import org.springframework.stereotype.Service;

@Service
public class RewardFeedbackMapper {

    public static final String REWARD_NOTIFICATION_FEEDBACK_STATUS_ACCEPTED = "ACCEPTED";
    public static final String REWARD_NOTIFICATION_FEEDBACK_STATUS_REJECTED = "REJECTED";

    public RewardFeedbackDTO apply(RewardsNotification notification, long deltaRewardCents){
        return RewardFeedbackDTO.builder()
                .rewardNotificationId(notification.getId())
                .initiativeId(notification.getInitiativeId())
                .userId(notification.getUserId())
                .organizationId(notification.getOrganizationId())
                .status(transcodeStatus(notification))
                .rejectionCode(!RewardNotificationStatus.COMPLETED_OK.equals(notification.getStatus()) ? notification.getResultCode() : null)
                .rejectionReason(notification.getRejectionReason())
                .rewardCents(notification.getRewardCents())
                .effectiveRewardCents(deltaRewardCents)
                .feedbackDate(notification.getFeedbackDate())
                .feedbackProgressive(notification.getFeedbackHistory().size())
                .executionDate(notification.getExecutionDate())
                .cro(notification.getCro())
                .build();
    }

    private static String transcodeStatus(RewardsNotification notification) {
        return switch (notification.getStatus()) {
            case COMPLETED_OK -> REWARD_NOTIFICATION_FEEDBACK_STATUS_ACCEPTED;
            case COMPLETED_KO, ERROR -> REWARD_NOTIFICATION_FEEDBACK_STATUS_REJECTED;
            default -> throw new IllegalArgumentException("Invalid notification status %s when sending %s".formatted(notification.getStatus(), notification.getId()));
        };
    }
}
