package it.gov.pagopa.reward.notification.service.csv.out.writer;

import it.gov.pagopa.reward.notification.dto.controller.FeedbackImportErrorsCsvDTO;

import java.util.List;

public interface FeedbackImportErrorsCsvService {

    String writeCsv(List<FeedbackImportErrorsCsvDTO> csvLines);
}
