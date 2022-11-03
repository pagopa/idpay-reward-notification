package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.dto.controller.ExportFilter;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import org.springframework.data.domain.Page;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RewardOrganizationExportsRepositoryExtended {

    Flux<RewardOrganizationExport> findAllByOrganizationIdAndInitiativeId(String organizationId, String initiativeId);

    Flux<RewardOrganizationExport> findAllWithFilters(String organizationId, String initiativeId, ExportFilter filters);

    Mono<Long> countAll(String organizationId, String initiativeId);

    Mono<Page<RewardOrganizationExport>> findAllPaged(String organizationId, String initiativeId);
}
