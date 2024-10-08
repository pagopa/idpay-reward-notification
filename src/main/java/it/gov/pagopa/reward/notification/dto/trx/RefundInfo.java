package it.gov.pagopa.reward.notification.dto.trx;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefundInfo {
    private List<TransactionProcessed> previousTrxs;
    private Map<String, PreviousReward> previousRewards;

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class PreviousReward {
        private String initiativeId;
        private String organizationId;
        private Long accruedRewardCents;
    }
}
