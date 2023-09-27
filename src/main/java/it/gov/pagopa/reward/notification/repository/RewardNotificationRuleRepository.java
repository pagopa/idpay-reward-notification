package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface RewardNotificationRuleRepository extends ReactiveMongoRepository<RewardNotificationRule, String>, RewardNotificationRuleRepositoryExtended {

    @Query(value = "{'accumulatedAmount':{$ne:null}, 'endDate':{ $gte:?0, $lt:?1 }}")
    Flux<RewardNotificationRule> findByAccumulatedAmountNotNullAndEndDateBetween(LocalDate from, LocalDate to);

    Mono<RewardNotificationRule> findByInitiativeIdAndOrganizationId(String initiativeId, String organizationId);
}
