package it.gov.pagopa.reward.notification.dto.controller;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class
FeedbackImportErrorsCsvDTO {

    @CsvBindByName(column = "row") private Integer row;
    @CsvBindByName(column = "errorCode") private String errorCode;
    @CsvBindByName(column = "errorDescription") private String errorDescription;
}
