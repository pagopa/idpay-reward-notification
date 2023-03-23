package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.model.SuspendedUser;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface SuspendedUsersRepository extends ReactiveMongoRepository<SuspendedUser, String> {

}
