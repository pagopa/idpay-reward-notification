package it.gov.pagopa.reward.notification.dto.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationImportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardImportsDTO {
    
    @JsonProperty("filePath")
    private String filePath;
    @JsonProperty("initiativeId")
    private String initiativeId;
    @JsonProperty("organizationId")
    private String organizationId;
    @JsonProperty("feedbackDate")
    private LocalDateTime feedbackDate;
    @JsonProperty("eTag")
    private String eTag;
    @JsonProperty("contentLength")
    private Integer contentLength;
    @JsonProperty("url")
    private String url;
    @JsonProperty("rewardsResulted")
    private Long rewardsResulted;
    @JsonProperty("rewardsResultedError")
    private Long rewardsResultedError;
    @JsonProperty("rewardsResultedOk")
    private Long rewardsResultedOk;
    @JsonProperty("rewardsResultedOkError")
    private Long rewardsResultedOkError;
    @JsonProperty("percentageResulted")
    private String percentageResulted;
    @JsonProperty("percentageResultedOk")
    private String percentageResultedOk;
    @JsonProperty("percentageResultedOkElab")
    private String percentageResultedOkElab;
    @JsonProperty("elabDate")
    private LocalDateTime elabDate;
    @JsonProperty("exportIds")
    private List<String> exportIds;
    @JsonProperty("status")
    private RewardOrganizationImportStatus status;
    @JsonProperty("errorsSize")
    private Integer errorsSize;
}
