package it.gov.pagopa.reward.notification.model;

import it.gov.pagopa.reward.notification.enums.OperationType;
import it.gov.pagopa.reward.notification.enums.RewardStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldNameConstants
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
    private Long rewardCents;
    private RewardStatus status;
    private String notificationId;

}
