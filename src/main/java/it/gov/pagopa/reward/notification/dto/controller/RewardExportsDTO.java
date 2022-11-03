package it.gov.pagopa.reward.notification.dto.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardExportsDTO {

    @JsonProperty("id")
    private String id;
    @JsonProperty("initiativeId")
    private String initiativeId;
    @JsonProperty("initiativeName")
    private String initiativeName;
    @JsonProperty("organizationId")
    private String organizationId;
    @JsonProperty("filePath")
    private String filePath;
    @JsonProperty("notificationDate")
    private LocalDateTime notificationDate;
    @JsonProperty("rewardsExported")
    private BigDecimal rewardsExported;
    @JsonProperty("rewardsResults")
    private BigDecimal rewardsResults;
    @JsonProperty("rewardsNotified")
    private BigDecimal rewardNotified;
    @JsonProperty("rewardsResulted")
    private BigDecimal rewardsResulted;
    @JsonProperty("feedbackDate")
    private LocalDateTime feedbackDate;
    @JsonProperty("status")
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
