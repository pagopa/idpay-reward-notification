package it.gov.pagopa.reward.notification.dto.rule;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.gov.pagopa.reward.notification.dto.AccumulatedAmountDTO;
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
}
