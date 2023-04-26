package it.gov.pagopa.reward.notification.controller;

import it.gov.pagopa.reward.notification.dto.controller.ExportFilter;
import it.gov.pagopa.reward.notification.dto.controller.FeedbackImportFilter;
import it.gov.pagopa.reward.notification.dto.controller.RewardExportsDTO;
import it.gov.pagopa.reward.notification.dto.controller.RewardImportsDTO;
import it.gov.pagopa.reward.notification.dto.controller.detail.*;
import it.gov.pagopa.reward.notification.exception.ClientExceptionNoBody;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.service.RewardsNotificationExpiredInitiativeHandlerService;
import it.gov.pagopa.reward.notification.service.exports.ForceOrganizationExportService;
import it.gov.pagopa.reward.notification.service.exports.OrganizationExportsServiceImpl;
import it.gov.pagopa.reward.notification.service.exports.detail.ExportDetailService;
import it.gov.pagopa.reward.notification.service.imports.OrganizationImportsServiceImpl;
import it.gov.pagopa.reward.notification.service.suspension.UserSuspensionService;
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

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
public class NotificationControllerImpl implements NotificationController {

    // region exports
    private final OrganizationExportsServiceImpl organizationExportsService;
    private final ForceOrganizationExportService forceOrganizationExportService;
    private final RewardsNotificationExpiredInitiativeHandlerService expiredInitiativeHandlerService;
    private final ExportDetailService exportDetailService;
    // endregion

    // region imports
    private final OrganizationImportsServiceImpl organizationImportsService;
    // endregion
    private final UserSuspensionService suspensionService;
    private final AuditUtilities auditUtilities;

    public NotificationControllerImpl(
            OrganizationExportsServiceImpl organizationExportsService,
            ForceOrganizationExportService forceOrganizationExportService,
            RewardsNotificationExpiredInitiativeHandlerService expiredInitiativeHandlerService,
            ExportDetailService exportDetailService,
            OrganizationImportsServiceImpl organizationImportsService,
            UserSuspensionService suspensionService, AuditUtilities auditUtilities) {
        this.organizationExportsService = organizationExportsService;
        this.forceOrganizationExportService = forceOrganizationExportService;
        this.expiredInitiativeHandlerService = expiredInitiativeHandlerService;
        this.exportDetailService = exportDetailService;
        this.organizationImportsService = organizationImportsService;
        this.suspensionService = suspensionService;
        this.auditUtilities = auditUtilities;
    }

    @Override
    public Flux<List<RewardOrganizationExport>> forceExportScheduling(LocalDate notificationDateToSearch) {
        log.info("Forcing rewardNotification csv export with notificationDateToSearch {}", notificationDateToSearch);
        return forceOrganizationExportService.execute(notificationDateToSearch);
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
        return exportDetailService.getExport(exportId, organizationId, initiativeId)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new ClientExceptionNoBody(HttpStatus.NOT_FOUND, "Cannot find export having id " + exportId))));
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
        return exportDetailService.getRewardNotification(notificationExternalId, organizationId, initiativeId)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new ClientExceptionNoBody(HttpStatus.NOT_FOUND,"Cannot find rewardNotification having external id " + notificationExternalId))));
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
                .switchIfEmpty(Mono.defer(() -> Mono.error(new ClientExceptionNoBody(HttpStatus.NOT_FOUND, "Cannot find import file " + fileName))));
    }

    @Override
    public Mono<ResponseEntity<Void>> suspendUserOnInitiative(String organizationId, String initiativeId, String userId) {
        return suspensionService.suspend(organizationId, initiativeId, userId)
                .map(u -> new ResponseEntity<Void>(HttpStatus.OK))
                .switchIfEmpty(Mono.error(new ClientExceptionNoBody(HttpStatus.NOT_FOUND, "Cannot find initiative having id " + initiativeId)));
    }

    @Override
    public Mono<ResponseEntity<Void>> readmitUserOnInitiative(String organizationId, String initiativeId, String userId) {
        return suspensionService.readmit(organizationId, initiativeId, userId)
                .map(u -> new ResponseEntity<Void>(HttpStatus.OK))
                .switchIfEmpty(Mono.error(new ClientExceptionNoBody(HttpStatus.NOT_FOUND, "Cannot find initiative having id " + initiativeId)));
    }

    private String buildImportId(String organizationId, String initiativeId, String fileName) {
        return "%s/%s/import/%s".formatted(organizationId, initiativeId, fileName);
    }
}
