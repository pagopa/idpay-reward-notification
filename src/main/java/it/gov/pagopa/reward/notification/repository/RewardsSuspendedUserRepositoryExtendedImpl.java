package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.model.RewardSuspendedUser;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Flux;

public class RewardsSuspendedUserRepositoryExtendedImpl implements RewardsSuspendedUserRepositoryExtended{
    private final ReactiveMongoTemplate mongoTemplate;

    public RewardsSuspendedUserRepositoryExtendedImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Flux<RewardSuspendedUser> findByInitiativeIdWithBatch(String initiativeId, int batchSize) {
        Query query = Query.query(Criteria.where(RewardSuspendedUser.Fields.initiativeId).is(initiativeId)).cursorBatchSize(batchSize);
        return mongoTemplate.find(query, RewardSuspendedUser.class);
    }
}
