package it.gov.pagopa.reward.notification.dto.trx;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.gov.pagopa.reward.notification.enums.OperationType;
import it.gov.pagopa.common.utils.json.BigDecimalScale2Deserializer;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id"}, callSuper = false)
@FieldNameConstants
public class TransactionDTO {
    private String idTrxAcquirer;

    private String acquirerCode;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime trxDate;

    private String hpan;

    private String operationType;

    private String circuitType;

    private String idTrxIssuer;

    private String correlationId;

    @JsonDeserialize(using = BigDecimalScale2Deserializer.class)
    private BigDecimal amount;

    private String amountCurrency;

    private String mcc;

    private String acquirerId;

    private String merchantId;

    private String terminalId;

    private String bin;

    private String senderCode;

    private String fiscalCode;

    private String vat;

    private String posType;

    private String par;

    private String userId;

    //region calculated fields
    private String id;
    private OperationType operationTypeTranscoded;
    @Builder.Default
    private List<String> rejectionReasons = new ArrayList<>();
    private BigDecimal effectiveAmount;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime trxChargeDate;
    private RefundInfo refundInfo;
    //endregion

}
