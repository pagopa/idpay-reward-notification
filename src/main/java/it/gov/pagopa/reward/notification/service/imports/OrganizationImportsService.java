package it.gov.pagopa.reward.notification.service.imports;

import it.gov.pagopa.reward.notification.dto.controller.ImportFilter;
import it.gov.pagopa.reward.notification.dto.controller.RewardImportsDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrganizationImportsService {

    Flux<RewardImportsDTO> findAllBy(String organizationId, String initiativeId, Pageable pageable, ImportFilter filters);

    Mono<Long> countAll(String organizationId, String initiativeId, ImportFilter filters);

    Mono<Page<RewardImportsDTO>> findAllPaged(String organizationId, String initiativeId, Pageable pageable, ImportFilter filters);
}
