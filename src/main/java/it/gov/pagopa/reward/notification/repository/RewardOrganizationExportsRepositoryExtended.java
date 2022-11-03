package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import reactor.core.publisher.Mono;

public interface RewardOrganizationExportsRepositoryExtended {
    Mono<RewardOrganizationExport> reserveExport();

    Mono<RewardOrganizationExport> configureNewExport(RewardOrganizationExport newExport);
}
