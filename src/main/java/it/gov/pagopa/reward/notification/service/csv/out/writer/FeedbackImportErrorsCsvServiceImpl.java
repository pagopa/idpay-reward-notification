package it.gov.pagopa.reward.notification.service.csv.out.writer;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import it.gov.pagopa.reward.notification.dto.controller.FeedbackImportErrorsCsvDTO;
import it.gov.pagopa.common.utils.csv.HeaderColumnNameStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

@Service
@Slf4j
public class FeedbackImportErrorsCsvServiceImpl implements FeedbackImportErrorsCsvService {

    private final char csvSeparator;
    private final HeaderColumnNameStrategy<FeedbackImportErrorsCsvDTO> mappingStrategy;

    public FeedbackImportErrorsCsvServiceImpl(
            @Value("${app.csv.import.separator}") char csvSeparator
    ) {
        this.csvSeparator = csvSeparator;
        this.mappingStrategy = new HeaderColumnNameStrategy<>(FeedbackImportErrorsCsvDTO.class);
    }

    @Override
    public String writeCsv(List<FeedbackImportErrorsCsvDTO> csvLines) {

        try (StringWriter writer = new StringWriter()) {
            StatefulBeanToCsv<FeedbackImportErrorsCsvDTO> csvWriter = buildCsvWriter(writer);
            csvWriter.write(csvLines);
            return writer.toString();
        } catch (IOException | CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
            throw new IllegalStateException("[REWARD_NOTIFICATION_IMPORT_ERRORS_CSV] Cannot create csv writer", e);
        }
    }

    private StatefulBeanToCsv<FeedbackImportErrorsCsvDTO> buildCsvWriter(StringWriter writer) {
        return new StatefulBeanToCsvBuilder<FeedbackImportErrorsCsvDTO>(writer)
                .withMappingStrategy(mappingStrategy)
                .withSeparator(csvSeparator)
                .withLineEnd("\n")
                .build();
    }
}
