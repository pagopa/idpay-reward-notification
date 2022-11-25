package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.dto.controller.FeedbackImportFilter;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RewardOrganizationImportsRepositoryExtended {

    Flux<RewardOrganizationImport> findAllBy(String organizationId, String initiativeId, Pageable pageable, FeedbackImportFilter filters);
    Mono<Long> countAll(String organizationId, String initiativeId, FeedbackImportFilter filters);
    Mono<RewardOrganizationImport> findByImportId(String organizationId, String initiativeId, String importId);
}
