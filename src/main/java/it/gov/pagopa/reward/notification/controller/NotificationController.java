package it.gov.pagopa.reward.notification.controller;

import it.gov.pagopa.reward.notification.dto.controller.ExportFilter;
import it.gov.pagopa.reward.notification.dto.controller.RewardExportsDTO;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
}
