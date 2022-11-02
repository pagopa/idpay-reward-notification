package it.gov.pagopa.reward.notification.controller;

import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Flux;

@RequestMapping("/notification")
public interface NotificationController {

    @GetMapping(value = "/exports")
    Flux<RewardOrganizationExport> getExports();
}
