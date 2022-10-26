package it.gov.pagopa.reward.notification.dto.iban;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@ToString(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class IbanOutcomeDTO extends IbanRequestDTO {
    @JsonProperty("status")
    private String status;
}