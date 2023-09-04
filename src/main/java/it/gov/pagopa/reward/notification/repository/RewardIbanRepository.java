package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.model.RewardIban;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RewardIbanRepository extends ReactiveMongoRepository<RewardIban, String> {
    Mono<RewardIban> deleteByIdAndIban(String id, String iban);
    Flux<RewardIban> deleteByInitiativeId(String initiativeId);
}