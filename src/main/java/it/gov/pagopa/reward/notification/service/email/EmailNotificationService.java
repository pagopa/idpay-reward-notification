package it.gov.pagopa.reward.notification.service.email;

import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import reactor.core.publisher.Mono;

public interface EmailNotificationService {

    Mono<RewardOrganizationImport> send(RewardOrganizationImport organizationImport, String templateName, String subject);
}
