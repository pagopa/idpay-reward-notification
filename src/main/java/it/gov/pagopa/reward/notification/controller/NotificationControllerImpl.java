package it.gov.pagopa.reward.notification.controller;

import it.gov.pagopa.reward.notification.dto.controller.ExportFilter;
import it.gov.pagopa.reward.notification.dto.controller.RewardExportsDTO;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.service.csv.export.ExportCsvService;
import it.gov.pagopa.reward.notification.service.exports.OrganizationExportsServiceImpl;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.notify.RewardsNotificationExpiredInitiativeHandlerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
public class NotificationControllerImpl implements NotificationController{

    private final OrganizationExportsServiceImpl organizationExportsService;
    private final ExportCsvService exportCsvService;
    private final RewardsNotificationExpiredInitiativeHandlerService expiredInitiativeHandlerService;

    public NotificationControllerImpl(OrganizationExportsServiceImpl organizationExportsService, ExportCsvService exportCsvService, RewardsNotificationExpiredInitiativeHandlerService expiredInitiativeHandlerService) {
        this.organizationExportsService = organizationExportsService;
        this.exportCsvService = exportCsvService;
        this.expiredInitiativeHandlerService = expiredInitiativeHandlerService;
    }

    @Override
    public Flux<RewardOrganizationExport> forceExportScheduling() {
        log.info("Forcing rewardNotification csv export");
        return exportCsvService.execute();
    }

    @Override
    public Flux<RewardsNotification> forceExpiredInitiativesScheduling() {
        log.info("Forcing rewardNotification expired initiatives handling");
        return expiredInitiativeHandlerService.handle();
    }

    @Override
    public Flux<RewardExportsDTO> getExports(String organizationId, String initiativeId, Pageable pageable, ExportFilter filters) {
           return organizationExportsService
                   .findAllBy(organizationId, initiativeId, pageable, filters);
    }

    @Override
    public Mono<Long> getExportsCount(String organizationId, String initiativeId, ExportFilter filters) {
        return organizationExportsService
                .countAll(organizationId, initiativeId, filters)
                .switchIfEmpty(Mono.just(0L));
    }

    @Override
    public Mono<Page<RewardExportsDTO>> getExportsPaged(String organizationId, String initiativeId, Pageable pageable, ExportFilter filters) {
        return organizationExportsService
                .findAllPaged(organizationId, initiativeId, pageable, filters)
                .switchIfEmpty(Mono.just(Page.empty(pageable)));
    }
}
