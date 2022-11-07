package it.gov.pagopa.reward.notification.service.exports;

import it.gov.pagopa.reward.notification.dto.controller.ExportFilter;
import it.gov.pagopa.reward.notification.dto.controller.RewardExportsDTO;
import it.gov.pagopa.reward.notification.dto.mapper.RewardOrganizationExports2ExportsDTOMapper;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationExportsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class OrganizationExportsServiceImpl implements OrganizationExportsService {

    private final RewardOrganizationExportsRepository rewardOrganizationExportsRepository;
    private final RewardOrganizationExports2ExportsDTOMapper rewardOrganizationExports2ExportsDTOMapper;

    public OrganizationExportsServiceImpl(RewardOrganizationExportsRepository rewardOrganizationExportsRepository, RewardOrganizationExports2ExportsDTOMapper rewardOrganizationExports2ExportsDTOMapper) {
        this.rewardOrganizationExportsRepository = rewardOrganizationExportsRepository;
        this.rewardOrganizationExports2ExportsDTOMapper = rewardOrganizationExports2ExportsDTOMapper;
    }

    @Override
    public Flux<RewardExportsDTO> findAll(String organizationId, String initiativeId, Pageable pageable, ExportFilter filters) {
        return rewardOrganizationExportsRepository
                .findAll(organizationId, initiativeId, pageable, filters)
                .map(rewardOrganizationExports2ExportsDTOMapper);
    }

    @Override
    public Mono<Long> countAll(String organizationId, String initiativeId, Pageable pageable, ExportFilter filters) {
        return rewardOrganizationExportsRepository.countAll(organizationId, initiativeId, pageable, filters);
    }

    @Override
    public Mono<Page<RewardExportsDTO>> findAllPaged(String organizationId, String initiativeId, Pageable pageable) {
        // TODO map result
        /*return rewardOrganizationExportsRepository
                .findAllPaged(organizationId, initiativeId, pageable)
                .map(rewardOrganizationExports2ExportsDTOMapper);*/
        return null;
    }
}
