package it.gov.pagopa.reward.notification.dto.rule;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.*;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
public class AccumulatedAmountDTO {
    @JsonProperty("accumulatedType")
    private AccumulatedTypeEnum accumulatedType;

    @JsonProperty("refundThreshold")
    private BigDecimal refundThreshold;

    public enum AccumulatedTypeEnum {
        BUDGET_EXHAUSTED("BUDGET_EXHAUSTED"),
        THRESHOLD_REACHED("THRESHOLD_REACHED");
        private final String value;
        AccumulatedTypeEnum(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static AccumulatedTypeEnum fromValue(String text) {
            for (AccumulatedTypeEnum b : AccumulatedTypeEnum.values()) {
                if (String.valueOf(b.value).equals(text)) {
                    return b;
                }
            }
            return null;
        }
    }
}
