package it.gov.pagopa.reward.notification.controller;

import it.gov.pagopa.reward.notification.dto.controller.ExportFilter;
import it.gov.pagopa.reward.notification.dto.controller.RewardExportsDTO;
import it.gov.pagopa.reward.notification.dto.mapper.RewardOrganizationExports2ExportsDTOMapper;
import it.gov.pagopa.reward.notification.exception.ClientExceptionNoBody;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationExportsRepositoryExtended;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class NotificationControllerImpl implements NotificationController{

    private final RewardOrganizationExportsRepositoryExtended rewardOrganizationExportsRepository;
    private final RewardOrganizationExports2ExportsDTOMapper rewardOrganizationExports2ExportsDTOMapper;

    public NotificationControllerImpl(RewardOrganizationExportsRepositoryExtended rewardOrganizationExportsRepository, RewardOrganizationExports2ExportsDTOMapper rewardOrganizationExports2ExportsDTOMapper) {
        this.rewardOrganizationExportsRepository = rewardOrganizationExportsRepository;
        this.rewardOrganizationExports2ExportsDTOMapper = rewardOrganizationExports2ExportsDTOMapper;
    }

    @Override
    public Flux<RewardExportsDTO> getExports(String organizationId, String initiativeId, Pageable pageable, ExportFilter optionalFilters) {
        return rewardOrganizationExportsRepository
                .findAllByOrganizationIdAndInitiativeId(organizationId, initiativeId)
                .map(rewardOrganizationExports2ExportsDTOMapper)
                .switchIfEmpty(Mono.error(new ClientExceptionNoBody(HttpStatus.NOT_FOUND)));
    }

    @Override
    public Mono<Long> getExportsCount(String organizationId, String initiativeId, ExportFilter optionalFilters) {
        return rewardOrganizationExportsRepository
                .findAllByOrganizationIdAndInitiativeId(organizationId, initiativeId).count()
                .switchIfEmpty(Mono.error(new ClientExceptionNoBody(HttpStatus.NOT_FOUND)));
    }

    @Override
    public Mono<Page<RewardExportsDTO>> getExportsPaged(String organizationId, String initiativeId, Pageable pageable, ExportFilter optionalFilters) {
        return null;
    }
}
