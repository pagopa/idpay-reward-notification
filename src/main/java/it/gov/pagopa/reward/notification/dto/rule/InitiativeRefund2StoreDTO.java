package it.gov.pagopa.reward.notification.dto.rule;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class InitiativeRefund2StoreDTO {
    @JsonProperty("initiativeId")
    private String initiativeId;

    @JsonProperty("initiativeName")
    private String initiativeName;

    @JsonProperty("organizationId")
    private String organizationId;

    @JsonProperty("organizationVat")
    private String organizationVat;

    @JsonProperty("general")
    private InitiativeGeneralDTO general;

    @JsonProperty("refundRule")
    private InitiativeRefundRuleDTO refundRule;
}
