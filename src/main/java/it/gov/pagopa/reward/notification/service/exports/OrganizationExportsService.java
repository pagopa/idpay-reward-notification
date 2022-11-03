package it.gov.pagopa.reward.notification.service.exports;

import it.gov.pagopa.reward.notification.dto.controller.ExportFilter;
import it.gov.pagopa.reward.notification.dto.controller.RewardExportsDTO;
import org.springframework.data.domain.Page;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrganizationExportsService {

    Flux<RewardExportsDTO> findAllByOrganizationIdAndInitiativeId(String organizationId, String initiativeId);

    Flux<RewardExportsDTO> findAllWithFilters(String organizationId, String initiativeId, ExportFilter filters);

    Mono<Long> countAll(String organizationId, String initiativeId);

    Mono<Page<RewardExportsDTO>> findAllPaged(String organizationId, String initiativeId);
}
