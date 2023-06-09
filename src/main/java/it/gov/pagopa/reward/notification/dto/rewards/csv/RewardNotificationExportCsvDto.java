package it.gov.pagopa.reward.notification.dto.rewards.csv;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardNotificationExportCsvDto {

    @CsvIgnore private String id;

    @CsvBindByName(column="progressiveCode") private Long progressiveCode;
    @CsvBindByName(column="uniqueID") private String uniqueID;
    @CsvBindByName(column="fiscalCode") private String fiscalCode;
    @CsvBindByName(column="beneficiaryName") private String beneficiaryName;
    @CsvBindByName(column="iban") private String iban;
    @CsvBindByName(column="amount") private Long amount;
    @CsvBindByName(column="paymentReason") private String paymentReason;
    @CsvBindByName(column="initiativeName") private String initiativeName;
    @CsvBindByName(column="initiativeID") private String initiativeID;
    @CsvBindByName(column="startDatePeriod") private String startDatePeriod;
    @CsvBindByName(column="endDatePeriod") private String endDatePeriod;
    @CsvBindByName(column="organizationId") private String organizationId;
    @CsvBindByName(column="organizationFiscalCode") private String organizationFiscalCode;
    @CsvBindByName(column="checkIban") private String checkIban;
    @CsvBindByName(column="typologyReward") private String typologyReward;
    @CsvBindByName(column="RelatedPaymentID") private String relatedPaymentID;

}
