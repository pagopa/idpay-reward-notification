package it.gov.pagopa.reward.notification.dto.trx;

import it.gov.pagopa.reward.notification.enums.OperationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionProcessed {
    @Id
    private String id;

    private String idTrxAcquirer;

    private String acquirerCode;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime trxDate;

    private String operationType;

    private String acquirerId;

    private String userId;

    private String correlationId;

    private Long amountCents;

    private Map<String, Reward> rewards;

    private Long effectiveAmountCents;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime trxChargeDate;
    private OperationType operationTypeTranscoded;

    private LocalDateTime timestamp;
}

