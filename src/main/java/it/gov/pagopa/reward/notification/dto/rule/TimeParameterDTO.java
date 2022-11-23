package it.gov.pagopa.reward.notification.dto.rule;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class TimeParameterDTO {
    @JsonProperty("timeType")
    private TimeTypeEnum timeType;

    public enum TimeTypeEnum {
        CLOSED("CLOSED"),

        DAILY("DAILY"),

        WEEKLY("WEEKLY"),

        MONTHLY("MONTHLY"),

        QUARTERLY("QUARTERLY");

        private final String value;

        TimeTypeEnum(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static TimeTypeEnum fromValue(String text) {
            for (TimeTypeEnum b : TimeTypeEnum.values()) {
                if (String.valueOf(b.value).equals(text)) {
                    return b;
                }
            }
            return null;
        }
    }
}
