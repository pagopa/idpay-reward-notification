package it.gov.pagopa.reward.notification.model;

import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationImportCsvDto;
import it.gov.pagopa.reward.notification.enums.BeneficiaryType;
import it.gov.pagopa.reward.notification.enums.DepositType;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationImportResult;
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
    private String remedialId;
    private String ordinaryId;
    private String ordinaryExternalId;
    private String recoveredId;
    private String recoveredExternalId;
    private String initiativeId;
    private String initiativeName;
    private String organizationId;
    private String organizationFiscalCode;
    private String beneficiaryId;
    private BeneficiaryType beneficiaryType;
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
    private String resultCode;
    private String rejectionReason;
    private LocalDateTime feedbackDate;
    private LocalDateTime feedbackElaborationDate;
    @Builder.Default private List<RewardNotificationHistory> feedbackHistory = new ArrayList<>();
    private LocalDate executionDate;
    private String cro;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class RewardNotificationHistory{
        private String feedbackFilePath;
        private LocalDateTime feedbackDate;
        private RewardOrganizationImportResult result;
        private String rejectionReason;

        public static RewardNotificationHistory fromImportRow(RewardNotificationImportCsvDto row, RewardOrganizationImportResult rowResult, RewardOrganizationImport importRequest){
            RewardNotificationHistory out = new RewardNotificationHistory();
            out.setFeedbackFilePath(importRequest.getFilePath());
            out.setFeedbackDate(importRequest.getFeedbackDate());
            out.setResult(rowResult);
            out.setRejectionReason(row.getRejectionReason());
            return out;
        }
    }

}
