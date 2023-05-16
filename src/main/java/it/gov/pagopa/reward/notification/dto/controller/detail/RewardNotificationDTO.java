package it.gov.pagopa.reward.notification.dto.controller.detail;

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
public class RewardNotificationDTO {
    @JsonProperty(value = "eventId")
    private String eventId;
    @JsonProperty(value = "iban")
    private String iban;
    @JsonProperty(value = "amount")
    private BigDecimal amount;
    @JsonProperty(value = "status")
    private RewardNotificationStatus status;
}
