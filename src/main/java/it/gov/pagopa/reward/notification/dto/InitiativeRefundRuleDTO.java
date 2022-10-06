package it.gov.pagopa.reward.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder

public class InitiativeRefundRuleDTO {
    @JsonProperty("accumulatedAmount")
    private AccumulatedAmountDTO accumulatedAmount;

    @JsonProperty("timeParameter")
    private TimeParameterDTO timeParameter;

    @JsonProperty("additionalInfo")
    private RefundAdditionalInfoDTO additionalInfo;
}
