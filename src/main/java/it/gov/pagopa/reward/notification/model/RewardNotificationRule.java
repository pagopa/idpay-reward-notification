package it.gov.pagopa.reward.notification.model;

import it.gov.pagopa.reward.notification.dto.AccumulatedAmountDTO;
import it.gov.pagopa.reward.notification.dto.TimeParameterDTO;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "reward_notification_rule")
public class RewardNotificationRule {
    @Id
    private String initiativeId;
    private String initiativeName;
    private LocalDate endDate;
    private String organizationId;
    private String serviceId;
    private AccumulatedAmountDTO accumulatedAmount;
    private TimeParameterDTO timeParameter;
    private String organizationFiscalCode;
}
