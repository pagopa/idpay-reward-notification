package it.gov.pagopa.reward.notification.service.exports;

import it.gov.pagopa.reward.notification.dto.controller.ExportFilter;
import it.gov.pagopa.reward.notification.dto.controller.RewardExportsDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrganizationExportsService {

    Flux<RewardExportsDTO> findAll(String organizationId, String initiativeId, Pageable pageable, ExportFilter filters);

    Mono<Long> countAll(String organizationId, String initiativeId, Pageable pageable, ExportFilter filters);

    Mono<Page<RewardExportsDTO>> findAllPaged(String organizationId, String initiativeId, Pageable pageable);
}
