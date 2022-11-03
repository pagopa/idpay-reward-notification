package it.gov.pagopa.reward.notification.controller;

import it.gov.pagopa.reward.notification.dto.controller.ExportFilter;
import it.gov.pagopa.reward.notification.dto.controller.RewardExportsDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequestMapping("idpay")
public interface NotificationController {

    @GetMapping(value = "/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports")
    Flux<RewardExportsDTO> getExports(@PathVariable("organizationId") String organizationId, @PathVariable("initiativeId") String initiativeId, Pageable pageable, ExportFilter optionalFilters);

    @GetMapping(value = "/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports/count")
    Mono<Long> getExportsCount(@PathVariable("organizationId") String organizationId, @PathVariable("initiativeId") String initiativeId, ExportFilter optionalFilters);

    @GetMapping(value = "/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports/paged")
    Mono<Page<RewardExportsDTO>> getExportsPaged(@PathVariable("organizationId") String organizationId, @PathVariable("initiativeId") String initiativeId, Pageable pageable, ExportFilter optionalFilters);
}
