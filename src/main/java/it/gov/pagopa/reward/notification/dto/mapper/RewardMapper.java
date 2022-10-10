package it.gov.pagopa.reward.notification.dto.mapper;

import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.enums.RewardStatus;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.Rewards;
import org.springframework.stereotype.Service;

@Service
public class RewardMapper {
    public Rewards apply(String initiativeId, Reward reward, RewardTransactionDTO trx, RewardNotificationRule rule) {
        return Rewards.builder()
                .id(buildRewardId(trx, initiativeId))
                .trxId(trx.getId())
                .userId(trx.getUserId())
                .initiativeId(initiativeId)
                .operationType(trx.getOperationTypeTranscoded())

                .reward(reward != null ? reward.getAccruedReward() : null)

                .organizationId(rule==null? null : rule.getOrganizationId())
                .status(rule==null? RewardStatus.REJECTED : RewardStatus.ACCEPTED)

                .build();
    }

    private String buildRewardId(RewardTransactionDTO trx, String initiativeId) {
        return "%s_%s".formatted(trx.getId(), initiativeId);
    }
}
