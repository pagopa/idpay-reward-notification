package it.gov.pagopa.reward.notification.dto.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationExportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SingleExportSummaryDTO {
    @JsonProperty(value = "createDate")
    private LocalDateTime createDate;
    @JsonProperty(value = "totalAmount")
    private BigDecimal totalAmount;
    @JsonProperty(value = "totalRefundedAmount")
    private BigDecimal totalRefundedAmount;
    @JsonProperty(value = "totalRefunds")
    private long totalRefunds;
    @JsonProperty(value = "successPercentage")
    private long successPercentage;
    @JsonProperty(value = "status")
    private RewardOrganizationExportStatus status;

}
