package it.gov.pagopa.reward.notification.dto.trx;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class Counters {
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);

    @Builder.Default
    private Long trxNumber = 0L;
    @Builder.Default
    private BigDecimal totalReward = ZERO;
    @Builder.Default
    private BigDecimal totalAmount = ZERO;
}
