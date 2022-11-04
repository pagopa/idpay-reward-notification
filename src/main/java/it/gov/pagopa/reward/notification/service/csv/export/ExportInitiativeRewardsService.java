package it.gov.pagopa.reward.notification.service.csv.export;

import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import reactor.core.publisher.Flux;

public interface ExportInitiativeRewardsService {
    Flux<RewardOrganizationExport> performExport(RewardOrganizationExport export);
}
