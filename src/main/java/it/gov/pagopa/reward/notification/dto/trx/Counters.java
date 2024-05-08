package it.gov.pagopa.reward.notification.dto.trx;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class Counters {

    @Builder.Default
    private Long trxNumber = 0L;
    @Builder.Default
    private Long totalRewardCents = 0L;
    @Builder.Default
    private Long totalAmountCents = 0L;
}
