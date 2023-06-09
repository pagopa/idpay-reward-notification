package it.gov.pagopa.reward.notification.dto.rewards.csv;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;
import com.opencsv.bean.CsvIgnore;
import it.gov.pagopa.common.utils.csv.LocalDateConverter;
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

    @CsvIgnore private Integer rowNumber;

    @CsvBindByName(column="uniqueID") private String uniqueID;
    @CsvBindByName(column="result") private String result;
    @CsvBindByName(column="rejectionReason") private String rejectionReason;
    @CsvBindByName(column="cro") private String cro;
    @CsvCustomBindByName(column="executionDate", converter = LocalDateConverter.class) private LocalDate executionDate;

}
