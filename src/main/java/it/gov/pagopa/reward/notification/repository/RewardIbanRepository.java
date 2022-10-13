package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.model.RewardIban;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface RewardIbanRepository extends ReactiveMongoRepository<RewardIban, String> {
}