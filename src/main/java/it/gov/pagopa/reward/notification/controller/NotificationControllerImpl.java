package it.gov.pagopa.reward.notification.controller;

import it.gov.pagopa.reward.notification.dto.controller.*;
import it.gov.pagopa.reward.notification.dto.controller.detail.*;
import it.gov.pagopa.reward.notification.exception.ClientExceptionNoBody;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.service.csv.out.ExportRewardNotificationCsvService;
import it.gov.pagopa.reward.notification.service.exports.OrganizationExportsServiceImpl;
import it.gov.pagopa.reward.notification.service.exports.detail.ExportDetailService;
import it.gov.pagopa.reward.notification.service.imports.OrganizationImportsServiceImpl;
import it.gov.pagopa.reward.notification.service.RewardsNotificationExpiredInitiativeHandlerService;
import it.gov.pagopa.reward.notification.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
public class NotificationControllerImpl implements NotificationController {

    // region exports
    private final OrganizationExportsServiceImpl organizationExportsService;
    private final ExportRewardNotificationCsvService exportRewardNotificationCsvService;
    private final RewardsNotificationExpiredInitiativeHandlerService expiredInitiativeHandlerService;
    private final ExportDetailService exportDetailService;
    // endregion

    // region imports
    private final OrganizationImportsServiceImpl organizationImportsService;
    private final AuditUtilities auditUtilities;
    // endregion

    public NotificationControllerImpl(
            OrganizationExportsServiceImpl organizationExportsService,
            ExportRewardNotificationCsvService exportRewardNotificationCsvService,
            RewardsNotificationExpiredInitiativeHandlerService expiredInitiativeHandlerService,
            ExportDetailService exportDetailService,
            OrganizationImportsServiceImpl organizationImportsService,
            AuditUtilities auditUtilities) {
        this.organizationExportsService = organizationExportsService;
        this.exportRewardNotificationCsvService = exportRewardNotificationCsvService;
        this.expiredInitiativeHandlerService = expiredInitiativeHandlerService;
        this.exportDetailService = exportDetailService;
        this.organizationImportsService = organizationImportsService;
        this.auditUtilities = auditUtilities;
    }

    @Override
    public Flux<RewardOrganizationExport> forceExportScheduling() {
        log.info("Forcing rewardNotification csv export");
        return exportRewardNotificationCsvService.execute();
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
        auditUtilities.logGetExportsPaged(initiativeId, organizationId);
        return organizationExportsService
                .findAllPaged(organizationId, initiativeId, pageable, filters)
                .switchIfEmpty(Mono.just(Page.empty(pageable)));
    }

    @Override
    public Mono<ExportSummaryDTO> getExport(String exportId, String organizationId, String initiativeId) {
        return exportDetailService.getExport(exportId, organizationId, initiativeId);
    }

    @Override
    public Flux<RewardNotificationDTO> getExportNotifications(String exportId, String organizationId, String initiativeId, ExportDetailFilter filters, Pageable pageable) {
        return exportDetailService.getExportNotifications(exportId, organizationId, initiativeId, filters, pageable);
    }

    @Override
    public Mono<ExportContentPageDTO> getExportNotificationsPaged(String exportId, String organizationId, String initiativeId, ExportDetailFilter filters, Pageable pageable) {
        return exportDetailService
                .getExportNotificationsPaged(exportId, organizationId, initiativeId, filters, pageable)
                .switchIfEmpty(exportDetailService.getExportNotificationEmptyPage(pageable));
    }

    @Override
    public Mono<RewardNotificationDetailDTO> getRewardNotification(String notificationExternalId, String organizationId, String initiativeId) {
        log.info("[REWARD_NOTIFICATION][NOTIFICATION_DETAIL][CONTROLLER] Get notification details with externalId {}, organizationId {} and initiativeId {}",
                notificationExternalId, organizationId, initiativeId);
        return exportDetailService.getRewardNotification(notificationExternalId, organizationId, initiativeId);
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
    public Mono<ResponseEntity<String>> getImportErrors(String organizationId, String initiativeId, String fileName) {
        return organizationImportsService
                .getErrorsCsvByImportId(organizationId, initiativeId, buildImportId(organizationId, initiativeId, fileName))
                .map(csv -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                        .body(csv))
                .switchIfEmpty(Mono.defer(() -> Mono.error(new ClientExceptionNoBody(HttpStatus.NOT_FOUND))));
    }

    private String buildImportId(String organizationId, String initiativeId, String fileName) {
        return "%s/%s/import/%s".formatted(organizationId, initiativeId, fileName);
    }
}
