package it.gov.pagopa.reward.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class InitiativeDTO {
    @JsonProperty("initiativeId")
    private String initiativeId;

    @JsonProperty("initiativeName")
    private String initiativeName;

    @JsonProperty("organizationId")
    private String organizationId;

    @JsonProperty("pdndToken")
    private String pdndToken;

    @JsonProperty("status")
    private String status;

    @JsonProperty("general")
    private InitiativeGeneralDTO general;

    @JsonProperty("additionalInfo")
    private InitiativeAdditionalDTO additionalInfo;

    @JsonProperty("refundRule")
    private InitiativeRefundRuleDTO refundRule;
}
