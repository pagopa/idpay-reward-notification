package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.dto.controller.ExportFilter;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RewardOrganizationExportsRepositoryExtended {

    Flux<RewardOrganizationExport> findAll(String organizationId, String initiativeId, Pageable pageable, ExportFilter filters);

    Mono<Long> countAll(String organizationId, String initiativeId, Pageable pageable, ExportFilter filters);

    Mono<Page<RewardOrganizationExport>> findAllPaged(String organizationId, String initiativeId, Pageable pageable);
}
