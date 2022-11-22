package it.gov.pagopa.reward.notification.dto.rewards;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardFeedbackDTO {
    private String initiativeId;
    private String userId;
    private String organizationId;
    private String rewardNotificationId;
    private String status;
    private String rejectionCode;
    private String rejectionReason;
    private Long rewardCents;
    private Long effectiveRewardCents;
    private LocalDateTime feedbackDate;
    private Integer feedbackProgressive;
    private LocalDate executionDate;
    private String cro;
}
