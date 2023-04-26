package it.gov.pagopa.reward.notification.service.exports;

import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.List;

public interface ForceOrganizationExportService {

    Flux<List<RewardOrganizationExport>> forceExecute(LocalDate notificationDateToSearch);
}
