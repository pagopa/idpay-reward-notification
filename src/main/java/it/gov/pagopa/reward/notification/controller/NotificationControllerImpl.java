package it.gov.pagopa.reward.notification.controller;

import it.gov.pagopa.reward.notification.dto.controller.*;
import it.gov.pagopa.reward.notification.dto.controller.detail.ExportDetailDTO;
import it.gov.pagopa.reward.notification.dto.controller.detail.ExportDetailFilter;
import it.gov.pagopa.reward.notification.dto.controller.detail.ExportPageDTO;
import it.gov.pagopa.reward.notification.dto.controller.detail.RefundDetailDTO;
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
public class NotificationControllerImpl implements NotificationController{

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
    public Mono<ExportSummaryDTO> getSingleExportSummary(String organizationId, String initiativeId, String exportId) {
        return exportDetailService.getExportSummary(organizationId, initiativeId, exportId);
    }

    @Override
    public Flux<ExportDetailDTO> getSingleExport(String organizationId, String initiativeId, String exportId, Pageable pageable, ExportDetailFilter filters) {
        return exportDetailService.getSingleExport(organizationId, initiativeId, exportId, pageable, filters);
    }

    @Override
    public Mono<ExportPageDTO> getSingleExportPaged(String organizationId, String initiativeId, String exportId, Pageable pageable, ExportDetailFilter filters) {
        return exportDetailService
                .getSingleExportPaged(organizationId, initiativeId, exportId, pageable, filters)
                .switchIfEmpty(exportDetailService.getExportDetailEmptyPage(pageable));
    }

    @Override
    public Mono<RefundDetailDTO> getSingleRefund(String organizationId, String initiativeId, String exportId, String eventId) {
        return exportDetailService.getSingleRefundDetail(organizationId, initiativeId, eventId);
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
                .map(csv->ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,  ContentDisposition.attachment().filename(fileName).build().toString())
                        .body(csv))
                .switchIfEmpty(Mono.defer(() -> Mono.error(new ClientExceptionNoBody(HttpStatus.NOT_FOUND))));
    }

    private String buildImportId(String organizationId, String initiativeId, String fileName) {
        return "%s/%s/import/%s".formatted(organizationId, initiativeId, fileName);
    }
}
