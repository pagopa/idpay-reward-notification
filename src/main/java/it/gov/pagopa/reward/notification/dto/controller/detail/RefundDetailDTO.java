package it.gov.pagopa.reward.notification.dto.controller.detail;

import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefundDetailDTO {
    private String fiscalCode;
    private String iban;
    private BigDecimal amount;
    private LocalDate startDate;
    private LocalDate endDate;
    private RewardNotificationStatus status;
    private RefundDetailType refundType;
    private String cro;
    private LocalDate transferDate;
    private LocalDate userNotificationDate;

    public enum RefundDetailType {
        ORDINARY,
        REMEDIAL
    }
}
