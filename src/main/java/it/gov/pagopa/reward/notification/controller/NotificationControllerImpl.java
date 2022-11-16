package it.gov.pagopa.reward.notification.controller;

import it.gov.pagopa.reward.notification.dto.controller.ExportFilter;
import it.gov.pagopa.reward.notification.dto.controller.RewardExportsDTO;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.service.csv.out.ExportRewardNotificationCsvService;
import it.gov.pagopa.reward.notification.service.exports.OrganizationExportsServiceImpl;
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
    private final ExportRewardNotificationCsvService exportRewardNotificationCsvService;

    public NotificationControllerImpl(OrganizationExportsServiceImpl organizationExportsService, ExportRewardNotificationCsvService exportRewardNotificationCsvService) {
        this.organizationExportsService = organizationExportsService;
        this.exportRewardNotificationCsvService = exportRewardNotificationCsvService;
    }

    @Override
    public Flux<RewardOrganizationExport> forceExportScheduling() {
        log.info("Forcing rewardNotification csv export");
        return exportRewardNotificationCsvService.execute();
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
