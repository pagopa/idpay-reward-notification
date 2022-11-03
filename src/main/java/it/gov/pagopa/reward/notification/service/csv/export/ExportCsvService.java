package it.gov.pagopa.reward.notification.service.csv.export;

import reactor.core.publisher.Mono;

public interface ExportCsvService {
    Mono<?> execute();
}
