package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.dto.rule.AccumulatedAmountDTO;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

public interface RewardNotificationRuleRepository extends ReactiveMongoRepository<RewardNotificationRule, String> {

    Flux<RewardNotificationRule> findByAccumulatedAmountIsNotAndEndDateGreaterThanEqualsAndEndDateLessThan(AccumulatedAmountDTO accumulatedAmount, LocalDate from, LocalDate to);
}
