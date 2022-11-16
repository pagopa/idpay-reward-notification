package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

public interface RewardNotificationRuleRepository extends ReactiveMongoRepository<RewardNotificationRule, String> {

    @Query(value = "{'endDate':{ $gte:?0, $lt:?1 }}")
    Flux<RewardNotificationRule> findByAccumulatedAmountNotNullAndEndDateBetween(LocalDate from, LocalDate to);
}
