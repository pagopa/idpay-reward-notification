package it.gov.pagopa.reward.notification.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.gov.pagopa.reward.notification.enums.OperationType;
import it.gov.pagopa.reward.notification.enums.RewardStatus;
import it.gov.pagopa.common.utils.json.BigDecimalScale2Deserializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "rewards")
public class Rewards {

    @Id
    private String id;
    private String trxId;
    private String userId;
    private String merchantId;
    private String initiativeId;
    private String organizationId;
    private OperationType operationType;
    @JsonDeserialize(using = BigDecimalScale2Deserializer.class)
    private BigDecimal reward;
    private RewardStatus status;
    private String notificationId;

}
