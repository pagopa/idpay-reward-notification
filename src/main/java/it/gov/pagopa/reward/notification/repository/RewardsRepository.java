package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.model.Rewards;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface RewardsRepository extends ReactiveMongoRepository<Rewards, String> {
}
