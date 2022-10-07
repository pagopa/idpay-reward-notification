package it.gov.pagopa.reward.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder

public class InitiativeGeneralDTO {
    @JsonProperty("endDate")
    private LocalDate endDate;
}
