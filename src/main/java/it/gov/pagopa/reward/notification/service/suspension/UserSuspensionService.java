package it.gov.pagopa.reward.notification.service.suspension;

import reactor.core.publisher.Mono;

public interface UserSuspensionService {

    Mono<Void> suspend(String organizationId, String initiativeId, String userId);

    Mono<Boolean> isNotSuspendedUser(String initiativeId, String userId);
}
