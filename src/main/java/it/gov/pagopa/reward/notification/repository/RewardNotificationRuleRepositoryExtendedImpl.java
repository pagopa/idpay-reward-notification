package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Flux;

public class RewardNotificationRuleRepositoryExtendedImpl implements RewardNotificationRuleRepositoryExtended{
    private final ReactiveMongoTemplate mongoTemplate;

    public RewardNotificationRuleRepositoryExtendedImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Flux<RewardNotificationRule> findByIdWithBatch(String initiativeId, int batchSize) {
        Query query = Query.query(Criteria.where(RewardNotificationRule.Fields.initiativeId).is(initiativeId)).cursorBatchSize(batchSize);
        return mongoTemplate.find(query, RewardNotificationRule.class);
    }
}
