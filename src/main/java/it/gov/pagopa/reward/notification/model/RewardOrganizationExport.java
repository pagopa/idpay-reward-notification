package it.gov.pagopa.reward.notification.model;

import it.gov.pagopa.reward.notification.enums.RewardOrganizationExportStatus;
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
@Builder(toBuilder = true)
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
    private LocalDate exportDate;
    private long progressive;

    /** Total reward notified in cents */
    private Long rewardsExportedCents;
    /** Total reward having result OK in cents  */
    private Long rewardsResultsCents;

    /** Total number of rewards notified */
    private Long rewardNotified;
    /** Total number of rewards having a result (both OK and KO) */
    private Long rewardsResulted;
    /** Total number of rewards having result OK */
    private Long rewardsResultedOk;

    /**  The percentage of {@link #rewardsResulted} compared to {@link #rewardNotified}.<br /> Expressed as an integer in cents: 1% → 100, 100% → 10000 */
    private Long percentageResulted;
    /**  The percentage of {@link #rewardsResultedOk} compared to {@link #rewardNotified}.<br /> Expressed as an integer in cents: 1% → 100, 100% → 10000 */
    private Long percentageResultedOk;
    /**  The percentage of {@link #rewardsResultsCents} compared to {@link #rewardsExportedCents}.<br /> Expressed as an integer in cents: 1% → 100, 100% → 10000 */
    private Long percentageResults;

    private LocalDateTime feedbackDate;
    private RewardOrganizationExportStatus status;
}
