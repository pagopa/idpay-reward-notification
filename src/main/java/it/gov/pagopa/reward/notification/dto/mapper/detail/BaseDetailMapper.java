package it.gov.pagopa.reward.notification.dto.mapper.detail;

import java.math.BigDecimal;

public interface BaseDetailMapper {

    default BigDecimal centsToEur(Long cents) {
        return BigDecimal.valueOf(cents/100);
    }
}
