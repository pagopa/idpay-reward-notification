package it.gov.pagopa.reward.notification.service.exports;

import it.gov.pagopa.reward.notification.dto.controller.ExportFilter;
import it.gov.pagopa.reward.notification.dto.controller.RewardExportsDTO;
import it.gov.pagopa.reward.notification.dto.mapper.RewardOrganizationExports2ExportsDTOMapper;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationExportsRepository;
import org.springframework.data.domain.Page;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class OrganizationExportsServiceImpl implements OrganizationExportsService {

    private final RewardOrganizationExportsRepository rewardOrganizationExportsRepository;
    private final RewardOrganizationExports2ExportsDTOMapper rewardOrganizationExports2ExportsDTOMapper;

    public OrganizationExportsServiceImpl(RewardOrganizationExportsRepository rewardOrganizationExportsRepository, RewardOrganizationExports2ExportsDTOMapper rewardOrganizationExports2ExportsDTOMapper) {
        this.rewardOrganizationExportsRepository = rewardOrganizationExportsRepository;
        this.rewardOrganizationExports2ExportsDTOMapper = rewardOrganizationExports2ExportsDTOMapper;
    }

    @Override
    public Flux<RewardExportsDTO> findAllByOrganizationIdAndInitiativeId(String organizationId, String initiativeId) {
        return rewardOrganizationExportsRepository
                .findAllByOrganizationIdAndInitiativeId(organizationId, initiativeId)
                .map(rewardOrganizationExports2ExportsDTOMapper);
    }

    @Override
    public Flux<RewardExportsDTO> findAllWithFilters(String organizationId, String initiativeId, ExportFilter filters) {
        // TODO map result
        return rewardOrganizationExportsRepository
                .findAllWithFilters(organizationId, initiativeId, filters);
    }

    @Override
    public Mono<Long> countAll(String organizationId, String initiativeId) {
        return rewardOrganizationExportsRepository.countAll(organizationId, initiativeId);
    }

    @Override
    public Mono<Page<RewardExportsDTO>> findAllPaged(String organizationId, String initiativeId) {
        // TODO map result
        return rewardOrganizationExportsRepository
                .findAllPaged(organizationId, initiativeId);
    }
}
