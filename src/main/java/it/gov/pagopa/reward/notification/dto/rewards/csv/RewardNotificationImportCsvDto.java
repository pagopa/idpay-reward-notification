package it.gov.pagopa.reward.notification.dto.rewards.csv;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class RewardNotificationImportCsvDto {

    @CsvBindByName(column="uniqueID") private String uniqueID;
    @CsvBindByName(column="result") private String result;
    @CsvBindByName(column="rejectionReason") private String rejectionReason;
    @CsvBindByName(column="cro") private String cro;
    @CsvBindByName(column="executionDate") private LocalDate executionDate;

}
