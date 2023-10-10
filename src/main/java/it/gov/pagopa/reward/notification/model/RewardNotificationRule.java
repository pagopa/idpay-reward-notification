package it.gov.pagopa.reward.notification.model;

import it.gov.pagopa.reward.notification.dto.rule.AccumulatedAmountDTO;
import it.gov.pagopa.reward.notification.dto.rule.TimeParameterDTO;
import it.gov.pagopa.reward.notification.enums.InitiativeRewardType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldNameConstants
@Document(collection = "reward_notification_rule")
public class RewardNotificationRule {
    @Id
    private String initiativeId;
    private String initiativeName;
    private LocalDate endDate;
    private String organizationId;
    private AccumulatedAmountDTO accumulatedAmount;
    private TimeParameterDTO timeParameter;
    private String organizationFiscalCode;
    private LocalDateTime updateDate;
    private InitiativeRewardType initiativeRewardType;
}
