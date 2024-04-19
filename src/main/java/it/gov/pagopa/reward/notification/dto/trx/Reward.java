package it.gov.pagopa.reward.notification.dto.trx;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Reward {
    /** The ruleEngine reward calculated */
    private Long providedRewardCents;
    /** The effective reward after CAP and REFUND evaluation */
    private Long accruedRewardCents;
    /** True, if the reward has been capped due to budget threshold */
    private boolean capped;

    /** True, if the reward has been capped due to daily threshold */
    private boolean dailyCapped;
    /** True, if the reward has been capped due to monthly threshold */
    private boolean monthlyCapped;
    /** True, if the reward has been capped due to yearly threshold */
    private boolean yearlyCapped;
    /** True, if the reward has been capped due to weekly threshold */
    private boolean weeklyCapped;

    /** True if is the trx has not more reward for the current initiative */
    private boolean isCompleteRefund;

    /** Counters */
    private RewardCounters counters = new RewardCounters();

    public Reward(Long rewardCents){
        this.providedRewardCents =rewardCents;
        this.accruedRewardCents =rewardCents;
        this.capped=false;
    }

    public Reward(Long providedRewardCents, Long accruedRewardCents){
        this.providedRewardCents = providedRewardCents;
        this.accruedRewardCents = accruedRewardCents;
        this.capped= providedRewardCents.compareTo(accruedRewardCents)!=0;
    }

    public Reward(Long providedRewardCents, Long accruedRewardCents, boolean capped){
        this.providedRewardCents = providedRewardCents;
        this.accruedRewardCents = accruedRewardCents;
        this.capped=capped;
    }
}
