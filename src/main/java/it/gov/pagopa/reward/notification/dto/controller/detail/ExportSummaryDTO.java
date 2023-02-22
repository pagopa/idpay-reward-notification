package it.gov.pagopa.reward.notification.dto.controller.detail;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationExportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExportSummaryDTO {
    @JsonProperty(value = "createDate")
    private LocalDate createDate;
    @JsonProperty(value = "totalAmount")
    private BigDecimal totalAmount;
    @JsonProperty(value = "totalRefundedAmount")
    private BigDecimal totalRefundedAmount;
    @JsonProperty(value = "totalRefunds")
    private long totalRefunds;
    @JsonProperty(value = "successPercentage")
    private String successPercentage;
    @JsonProperty(value = "status")
    private RewardOrganizationExportStatus status;

}
