package it.gov.pagopa.reward.notification.controller;

import it.gov.pagopa.reward.notification.dto.controller.ExportFilter;
import it.gov.pagopa.reward.notification.dto.controller.FeedbackImportFilter;
import it.gov.pagopa.reward.notification.dto.controller.RewardExportsDTO;
import it.gov.pagopa.reward.notification.dto.controller.RewardImportsDTO;
import it.gov.pagopa.reward.notification.exception.ClientExceptionNoBody;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.service.csv.out.ExportRewardNotificationCsvService;
import it.gov.pagopa.reward.notification.service.exports.OrganizationExportsServiceImpl;
import it.gov.pagopa.reward.notification.service.imports.OrganizationImportsServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
public class NotificationControllerImpl implements NotificationController{

    // region exports
    private final OrganizationExportsServiceImpl organizationExportsService;
    private final ExportRewardNotificationCsvService exportRewardNotificationCsvService;
    // endregion

    // region imports
    private final OrganizationImportsServiceImpl organizationImportsService;
    // endregion
    public NotificationControllerImpl(OrganizationExportsServiceImpl organizationExportsService, ExportRewardNotificationCsvService exportRewardNotificationCsvService, OrganizationImportsServiceImpl organizationImportsService) {
        this.organizationExportsService = organizationExportsService;
        this.exportRewardNotificationCsvService = exportRewardNotificationCsvService;
        this.organizationImportsService = organizationImportsService;
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

    @Override
    public Flux<RewardImportsDTO> getImports(String organizationId, String initiativeId, Pageable pageable, FeedbackImportFilter filters) {
        return organizationImportsService
                .findAllBy(organizationId, initiativeId, pageable, filters);
    }

    @Override
    public Mono<Long> getImportsCount(String organizationId, String initiativeId, FeedbackImportFilter filters) {
        return organizationImportsService
                .countAll(organizationId, initiativeId, filters)
                .switchIfEmpty(Mono.just(0L));
    }

    @Override
    public Mono<Page<RewardImportsDTO>> getImportsPaged(String organizationId, String initiativeId, Pageable pageable, FeedbackImportFilter filters) {
        return organizationImportsService
                .findAllPaged(organizationId, initiativeId, pageable, filters)
                .switchIfEmpty(Mono.just(Page.empty(pageable)));
    }

    @Override
    public Mono<String> getImportErrors(String organizationId, String initiativeId, String fileName) {
        return organizationImportsService
                .getErrorsCsvByImportId(organizationId, initiativeId, buildImportId(organizationId, initiativeId, fileName))
                .switchIfEmpty(Mono.defer(() -> Mono.error(new ClientExceptionNoBody(HttpStatus.NOT_FOUND))));
    }

    private String buildImportId(String organizationId, String initiativeId, String fileName) {
        return "%s/%s/import/%s".formatted(organizationId, initiativeId, fileName);
    }
}
