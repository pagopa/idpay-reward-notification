package it.gov.pagopa.reward.notification.dto.mapper;

import it.gov.pagopa.reward.notification.dto.rewards.RewardFeedbackDTO;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.utils.Utils;
import org.springframework.stereotype.Service;

@Service
public class RewardFeedbackMapper {

    public static final String REWARD_NOTIFICATION_FEEDBACK_STATUS_ACCEPTED = "ACCEPTED";
    public static final String REWARD_NOTIFICATION_FEEDBACK_STATUS_REJECTED = "REJECTED";

    public RewardFeedbackDTO apply(RewardsNotification notification, long deltaRewardCents){
        return RewardFeedbackDTO.builder()
                .id(notification.getId())
                .externalId(notification.getExternalId())
                .rewardNotificationId(notification.getId())
                .initiativeId(notification.getInitiativeId())
                .userId(notification.getBeneficiaryId())
                .organizationId(notification.getOrganizationId())
                .iban(notification.getIban())
                .status(transcodeStatus(notification))
                .rewardStatus(notification.getStatus())
                .rejectionCode(!RewardNotificationStatus.COMPLETED_OK.equals(notification.getStatus()) ? notification.getResultCode() : null)
                .rejectionReason(notification.getRejectionReason())
                .refundType(Utils.getRefundType(notification))
                .effectiveRewardCents(notification.getRewardCents())
                .rewardCents(deltaRewardCents)
                .startDate(notification.getStartDepositDate())
                .endDate(notification.getNotificationDate())
                .feedbackDate(notification.getFeedbackDate())
                .feedbackProgressive(notification.getFeedbackHistory().size())
                .executionDate(notification.getExecutionDate())
                .transferDate(notification.getExecutionDate())
                .userNotificationDate(notification.getFeedbackElaborationDate() != null
                        ? notification.getFeedbackElaborationDate().toLocalDate()
                        : null)
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
