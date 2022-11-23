package it.gov.pagopa.reward.notification.model;

import it.gov.pagopa.reward.notification.enums.DepositType;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Document(collection = "rewards_notification")
@FieldNameConstants
public class RewardsNotification {

    @Id
    private String id;
    private String externalId;
    private String initiativeId;
    private String initiativeName;
    private String organizationId;
    private String organizationFiscalCode;
    private String userId;
    private Long progressive;
    private Long rewardCents;
    @Builder.Default
    private List<String> trxIds=new ArrayList<>();
    private DepositType depositType;
    private LocalDate startDepositDate;
    /** The notification date searched */
    private LocalDate notificationDate;
    /** The export creation date  */
    private LocalDateTime exportDate;
    private String exportId;
    private String iban;
    private String checkIbanResult;
    private RewardNotificationStatus status;
    private String rejectionCode;
    private String rejectionReason;
    private LocalDateTime feedbackDate;
    private List<RewardNotificationHistory> feedbackHistory;
    private LocalDateTime orderDate;
    private LocalDateTime executionDate;
    private String trn;
    private String cro;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RewardNotificationHistory{
        private LocalDateTime timestamp;
        private String status;
        private String rejectionCode;
        private String rejectionReason;
    }

}
