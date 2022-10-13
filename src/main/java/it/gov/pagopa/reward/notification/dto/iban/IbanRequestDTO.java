package it.gov.pagopa.reward.notification.dto.iban;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IbanRequestDTO {
    @JsonProperty("userId")
    String userId;
    @JsonProperty("initiativeId")
    String initiativeId;
    @JsonProperty("iban")
    String iban;
}