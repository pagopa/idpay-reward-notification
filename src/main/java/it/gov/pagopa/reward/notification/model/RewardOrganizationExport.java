package it.gov.pagopa.reward.notification.model;

import it.gov.pagopa.reward.notification.enums.ExportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "rewards_organization_exports")
@FieldNameConstants
public class RewardOrganizationExport {

    @Id
    private String id;
    private String initiativeId;
    private String initiativeName;
    private String organizationId;
    private String filePath;
    private LocalDate notificationDate;
    private Long rewardsExportedCents;
    private Long rewardsResultsCents;
    private Long rewardNotified;
    private Long rewardsResulted;
    private LocalDateTime feedbackDate;
    private ExportStatus status;
}
