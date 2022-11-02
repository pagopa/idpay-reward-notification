package it.gov.pagopa.reward.notification.controller;

import it.gov.pagopa.reward.notification.dto.controller.RewardExportsDTO;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("idpay")
public interface NotificationController {

    @GetMapping(value = "/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports")
    Flux<RewardExportsDTO> getExports(@PathVariable("organizationId") String organizationId, @PathVariable("initiativeId") String initiativeId);

    @GetMapping(value = "/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports_count")
    Mono<Long> getExportsCount(@PathVariable("organizationId") String organizationId, @PathVariable("initiativeId") String initiativeId);
}
