package it.gov.pagopa.reward.notification.controller;

import it.gov.pagopa.reward.notification.dto.controller.RewardExportsDTO;
import it.gov.pagopa.reward.notification.dto.mapper.RewardOrganizationExports2ExportsDTOMapper;
import it.gov.pagopa.reward.notification.exception.ClientExceptionNoBody;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationExportsRepository;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class NotificationControllerImpl implements NotificationController{

    private final RewardOrganizationExportsRepository rewardOrganizationExportsRepository;
    private final RewardOrganizationExports2ExportsDTOMapper rewardOrganizationExports2ExportsDTOMapper;

    public NotificationControllerImpl(RewardOrganizationExportsRepository rewardOrganizationExportsRepository, RewardOrganizationExports2ExportsDTOMapper rewardOrganizationExports2ExportsDTOMapper) {
        this.rewardOrganizationExportsRepository = rewardOrganizationExportsRepository;
        this.rewardOrganizationExports2ExportsDTOMapper = rewardOrganizationExports2ExportsDTOMapper;
    }

    @Override
    public Flux<RewardExportsDTO> getExports(String organizationId, String initiativeId) {
        // TODO Pageable
        return rewardOrganizationExportsRepository
                .findAllByOrganizationIdAndInitiativeId(organizationId, initiativeId)
                .map(rewardOrganizationExports2ExportsDTOMapper)
                .switchIfEmpty(Mono.error(new ClientExceptionNoBody(HttpStatus.NOT_FOUND)));
    }

    @Override
    public Mono<Long> getExportsCount(String organizationId, String initiativeId) {
        return rewardOrganizationExportsRepository
                .findAllByOrganizationIdAndInitiativeId(organizationId, initiativeId).count()
                .switchIfEmpty(Mono.error(new ClientExceptionNoBody(HttpStatus.NOT_FOUND)));
    }
}
