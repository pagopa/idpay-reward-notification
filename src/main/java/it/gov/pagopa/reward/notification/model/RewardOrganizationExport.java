package it.gov.pagopa.reward.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "rewards_organization_exports")
public class RewardOrganizationExport {

    @Id
    private String id;
    private String initiativeId;
    private String initiativeName;
    private String organizationId;
    private String filePath;
    private LocalDateTime notificationDate;
    private BigDecimal rewardsExported;
    private BigDecimal rewardsResults;
    private LocalDateTime feedbackDate;
    private String status;

    // region export's status
    public static final String STATUS_TODO = "TODO";
    public static final String STATUS_IN_PROGRESS = "IN PROGRESS";
    public static final String STATUS_EXPORTED = "EXPORTED";
    public static final String STATUS_READ = "READ";
    public static final String STATUS_PARTIAL = "PARTIAL";
    public static final String STATUS_COMPLETE = "COMPLETE";
    // endregion
}
