package it.gov.pagopa.reward.notification.controller;

import it.gov.pagopa.reward.notification.dto.controller.ExportFilter;
import it.gov.pagopa.reward.notification.dto.controller.FeedbackImportFilter;
import it.gov.pagopa.reward.notification.dto.controller.RewardExportsDTO;
import it.gov.pagopa.reward.notification.dto.controller.RewardImportsDTO;
import it.gov.pagopa.reward.notification.dto.controller.detail.*;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.model.SuspendedUser;
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

    @GetMapping(value = "/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports/{exportId}")
    Mono<ExportSummaryDTO> getExport(
            @PathVariable("exportId") String exportId,
            @PathVariable("organizationId") String organizationId,
            @PathVariable("initiativeId") String initiativeId);

    @GetMapping(value = "/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports/{exportId}/content")
    Flux<RewardNotificationDTO> getExportNotifications(
            @PathVariable("exportId") String exportId,
            @PathVariable("organizationId") String organizationId,
            @PathVariable("initiativeId") String initiativeId,
            ExportDetailFilter filters,
            @PageableDefault(size = 10) Pageable pageable);

    @GetMapping(value = "/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports/{exportId}/content/paged")
    Mono<ExportContentPageDTO> getExportNotificationsPaged(
            @PathVariable("exportId") String exportId,
            @PathVariable("organizationId") String organizationId,
            @PathVariable("initiativeId") String initiativeId,
            ExportDetailFilter filters,
            @PageableDefault(size = 10) Pageable pageable);

    @GetMapping(value = "/organization/{organizationId}/initiative/{initiativeId}/reward/notification/byExternalId/{notificationExternalId}")
    Mono<RewardNotificationDetailDTO> getRewardNotification(
            @PathVariable("notificationExternalId") String notificationExternalId,
            @PathVariable("organizationId") String organizationId,
            @PathVariable("initiativeId") String initiativeId);

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

    @GetMapping(value = "/organization/{organizationId}/initiative/{initiativeId}/user/{userId}/suspend")
    Mono<SuspendedUser> suspendUserOnInitiative(
            @PathVariable("organizationId") String organizationId,
            @PathVariable("initiativeId") String initiativeId,
            @PathVariable("userId") String userId);
}
