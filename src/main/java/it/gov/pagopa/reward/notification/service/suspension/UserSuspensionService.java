package it.gov.pagopa.reward.notification.service.suspension;

import it.gov.pagopa.reward.notification.model.SuspendedUser;
import reactor.core.publisher.Mono;

public interface UserSuspensionService {

    Mono<SuspendedUser> suspend(String organizationId, String initiativeId, String userId);

    Mono<Boolean> isNotSuspendedUser(String initiativeId, String userId);
}
