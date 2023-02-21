package it.gov.pagopa.reward.notification.dto.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExportDetailDTO {
    @JsonProperty(value = "id")
    private String id;
    @JsonProperty(value = "iban")
    private String iban;
    @JsonProperty(value = "amount")
    private BigDecimal amount;
    @JsonProperty(value = "status")
    private RewardNotificationStatus status;
}
