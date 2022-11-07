package it.gov.pagopa.reward.notification.controller;

import it.gov.pagopa.reward.notification.dto.controller.ExportFilter;
import it.gov.pagopa.reward.notification.dto.controller.RewardExportsDTO;
import it.gov.pagopa.reward.notification.exception.ClientExceptionNoBody;
import it.gov.pagopa.reward.notification.service.exports.OrganizationExportsServiceImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class NotificationControllerImpl implements NotificationController{

    private final OrganizationExportsServiceImpl organizationExportsService;

    public NotificationControllerImpl(OrganizationExportsServiceImpl organizationExportsService) {
        this.organizationExportsService = organizationExportsService;
    }

    @Override
    public Flux<RewardExportsDTO> getExports(String organizationId, String initiativeId, Pageable pageable, ExportFilter filters) {
           return organizationExportsService
                   .findAllBy(organizationId, initiativeId, pageable, filters)
                   .switchIfEmpty(Mono.error(new ClientExceptionNoBody(HttpStatus.NOT_FOUND)));
    }

    @Override
    public Mono<Long> getExportsCount(String organizationId, String initiativeId, Pageable pageable, ExportFilter filters) {
        return organizationExportsService
                .countAll(organizationId, initiativeId, pageable, filters)
                .switchIfEmpty(Mono.error(new ClientExceptionNoBody(HttpStatus.NOT_FOUND)));
    }

    @Override
    public Mono<Page<RewardExportsDTO>> getExportsPaged(String organizationId, String initiativeId, Pageable pageable, ExportFilter filters) {
        return organizationExportsService
                .findAllPaged(organizationId, initiativeId, pageable, filters)
                .switchIfEmpty(Mono.error(new ClientExceptionNoBody(HttpStatus.NOT_FOUND)));
    }
}