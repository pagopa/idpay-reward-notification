package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.dto.controller.ImportFilter;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RewardOrganizationImportsRepositoryExtended {

    Flux<RewardOrganizationImport> findAllBy(String organizationId, String initiativeId, Pageable pageable, ImportFilter filters);
    Mono<Long> countAll(String organizationId, String initiativeId, ImportFilter filters);
}
