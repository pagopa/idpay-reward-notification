package it.gov.pagopa.reward.notification.service.suspension;

import it.gov.pagopa.reward.notification.model.RewardSuspendedUser;
import reactor.core.publisher.Mono;

public interface UserSuspensionService {

    Mono<RewardSuspendedUser> suspend(String organizationId, String initiativeId, String userId);
    Mono<Boolean> isNotSuspendedUser(String initiativeId, String userId);

    Mono<RewardSuspendedUser> readmit(String organizationId, String initiativeId, String userId);
}
