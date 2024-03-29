package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.model.RewardSuspendedUser;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface RewardsSuspendedUserRepository extends ReactiveMongoRepository<RewardSuspendedUser, String>, RewardsSuspendedUserRepositoryExtended {

    Mono<RewardSuspendedUser> findByUserIdAndOrganizationIdAndInitiativeId(String userId, String organizationId, String initiativeId);
}
