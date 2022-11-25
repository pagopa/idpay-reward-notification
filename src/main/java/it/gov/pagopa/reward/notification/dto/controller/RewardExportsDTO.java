package it.gov.pagopa.reward.notification.dto.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationExportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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
    private LocalDate notificationDate;

    @JsonProperty("rewardsExported")
    private String rewardsExported;
    @JsonProperty("rewardsResults")
    private String rewardsResults;

    @JsonProperty("rewardsNotified")
    private Long rewardNotified;
    @JsonProperty("rewardsResulted")
    private Long rewardsResulted;
    @JsonProperty("rewardsResultedOk")
    private Long rewardsResultedOk;

    @JsonProperty("percentageResulted")
    private String percentageResulted;
    @JsonProperty("percentageResultedOk")
    private String percentageResultedOk;
    @JsonProperty("percentageResults")
    private String percentageResults;

    @JsonProperty("feedbackDate")
    private LocalDateTime feedbackDate;
    @JsonProperty("status")
    private RewardOrganizationExportStatus status;
}
