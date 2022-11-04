package it.gov.pagopa.reward.notification.service.csv.export;

import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import reactor.core.publisher.Flux;

public interface ExportCsvService {
    Flux<RewardOrganizationExport> execute();
}
