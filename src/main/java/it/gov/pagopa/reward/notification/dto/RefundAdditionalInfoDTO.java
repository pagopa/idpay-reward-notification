package it.gov.pagopa.reward.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class RefundAdditionalInfoDTO {
    @JsonProperty("identificationCode")
    private String identificationCode;
}
