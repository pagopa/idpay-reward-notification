package it.gov.pagopa.reward.notification.dto.rewards;

import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.utils.Utils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class RewardFeedbackDTO {
    private String id;
    private String externalId;
    private String initiativeId;
    private String userId;
    private String organizationId;
    private String rewardNotificationId;
    private String iban;
    private String status;
    private RewardNotificationStatus rewardStatus;
    private String rejectionCode;
    private String rejectionReason;
    private Utils.RefundType refundType;
    private Long rewardCents;
    private Long effectiveRewardCents;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime feedbackDate;
    private Integer feedbackProgressive;
    private LocalDate executionDate;
    private String cro;
    private LocalDate transferDate;
    private LocalDate userNotificationDate;
}
