package it.gov.pagopa.reward.notification.dto.controller.detail;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.utils.Utils;
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
public class RewardNotificationDetailDTO {
    @JsonProperty(value = "id")
    private String id;
    @JsonProperty(value = "externalId")
    private String externalId;
    @JsonProperty(value = "userId")
    private String userId;
    @JsonProperty(value = "beneficiaryType")
    private String beneficiaryType;
    @JsonProperty(value = "merchantFiscalCode")
    private String merchantFiscalCode;
    @JsonProperty(value = "iban")
    private String iban;
    @JsonProperty(value = "amount")
    private BigDecimal amount;
    @JsonProperty(value = "startDate")
    private LocalDate startDate;
    @JsonProperty(value = "endDate")
    private LocalDate endDate;
    @JsonProperty(value = "status")
    private RewardNotificationStatus status;
    @JsonProperty(value = "refundType")
    private Utils.RefundType refundType;
    @JsonProperty(value = "cro")
    private String cro;
    @JsonProperty(value = "transferDate")
    private LocalDate transferDate;
    @JsonProperty(value = "userNotificationDate")
    private LocalDate userNotificationDate;
}
