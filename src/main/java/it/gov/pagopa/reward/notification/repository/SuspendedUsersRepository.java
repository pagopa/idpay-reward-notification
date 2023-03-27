package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.model.SuspendedUser;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface SuspendedUsersRepository extends ReactiveMongoRepository<SuspendedUser, String> {

    Mono<SuspendedUser> findByUserIdAndOrganizationIdAndInitiativeId(String userId, String organizationId, String initiativeId);
}
