package it.gov.pagopa.reward.notification.dto.iban;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class IbanRequestDTO {
    @JsonProperty("userId")
    String userId;
    @JsonProperty("initiativeId")
    String initiativeId;
    @JsonProperty("iban")
    String iban;
}