package it.gov.pagopa.reward.notification.dto.controller;

import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SingleRefundDTO {
    private String fiscalCode;
    private String iban;
    private BigDecimal amount;
    private LocalDate startDate;
    private LocalDate endDate;
    private RewardNotificationStatus status;
    private String refundType;
    private String cro;
    private LocalDateTime creationDate;
    private LocalDateTime sendDate;
    private LocalDateTime notificationDate;
}
