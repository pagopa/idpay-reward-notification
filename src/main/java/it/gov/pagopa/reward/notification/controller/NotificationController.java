package it.gov.pagopa.reward.notification.controller;

import it.gov.pagopa.reward.notification.dto.controller.*;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequestMapping("/idpay")
public interface NotificationController {

    @GetMapping("/reward/notification/exports/start")
    Flux<RewardOrganizationExport> forceExportScheduling();

    @GetMapping("/reward/notification/expired-initiatives/start")
    Flux<RewardsNotification> forceExpiredInitiativesScheduling();

    @GetMapping(value = "/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports")
    Flux<RewardExportsDTO> getExports(
            @PathVariable("organizationId") String organizationId,
            @PathVariable("initiativeId") String initiativeId,
            @PageableDefault(size = 10) Pageable pageable,
            ExportFilter filters);

    @GetMapping(value = "/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports/count")
    Mono<Long> getExportsCount(
            @PathVariable("organizationId") String organizationId,
            @PathVariable("initiativeId") String initiativeId,
            ExportFilter filters);

    @GetMapping(value = "/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports/paged")
    Mono<Page<RewardExportsDTO>> getExportsPaged(
            @PathVariable("organizationId") String organizationId,
            @PathVariable("initiativeId") String initiativeId,
            @PageableDefault(size = 10) Pageable pageable,
            ExportFilter filters);

    @GetMapping(value = "/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports/{exportId}/summary")
    Mono<ExportSummaryDTO> getSingleExportSummary(
            @PathVariable("organizationId") String organizationId,
            @PathVariable("initiativeId") String initiativeId,
            @PathVariable("exportId") String exportId);

    @GetMapping(value = "/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports/{exportId}")
    Flux<ExportDetailDTO> getSingleExport(
            @PathVariable("organizationId") String organizationId,
            @PathVariable("initiativeId") String initiativeId,
            @PathVariable("exportId") String exportId,
            @PageableDefault(size = 10) Pageable pageable,
            SingleExportFilter filters);

    @GetMapping(value = "/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports/{exportId}/paged")
    Mono<ExportPageDTO> getSingleExportPaged(
            @PathVariable("organizationId") String organizationId,
            @PathVariable("initiativeId") String initiativeId,
            @PathVariable("exportId") String exportId,
            @PageableDefault(size = 10) Pageable pageable,
            SingleExportFilter filters);

    @GetMapping(value = "/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports/{exportId}/refund/{eventId}")
    Mono<SingleRefundDTO> getSingleRefund(
            @PathVariable("organizationId") String organizationId,
            @PathVariable("initiativeId") String initiativeId,
            @PathVariable("exportId") String exportId,
            @PathVariable("eventId") String eventId);

    @GetMapping(value = "/organization/{organizationId}/initiative/{initiativeId}/reward/notification/imports")
    Flux<RewardImportsDTO> getImports(
            @PathVariable("organizationId") String organizationId,
            @PathVariable("initiativeId") String initiativeId,
            @PageableDefault(size = 10) Pageable pageable,
            FeedbackImportFilter filters);

    @GetMapping(value = "/organization/{organizationId}/initiative/{initiativeId}/reward/notification/imports/count")
    Mono<Long> getImportsCount(
            @PathVariable("organizationId") String organizationId,
            @PathVariable("initiativeId") String initiativeId,
            FeedbackImportFilter filters);

    @GetMapping(value = "/organization/{organizationId}/initiative/{initiativeId}/reward/notification/imports/paged")
    Mono<Page<RewardImportsDTO>> getImportsPaged(
            @PathVariable("organizationId") String organizationId,
            @PathVariable("initiativeId") String initiativeId,
            @PageableDefault(size = 10) Pageable pageable,
            FeedbackImportFilter filters);

    @GetMapping(value = "/organization/{organizationId}/initiative/{initiativeId}/reward/notification/imports/{fileName}/errors", produces = "text/csv")
    Mono<ResponseEntity<String>> getImportErrors(
            @PathVariable("organizationId") String organizationId,
            @PathVariable("initiativeId") String initiativeId,
            @PathVariable("fileName") String fileName);
}
