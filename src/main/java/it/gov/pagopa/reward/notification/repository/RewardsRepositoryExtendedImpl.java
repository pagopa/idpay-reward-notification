package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.model.Rewards;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Flux;

public class RewardsRepositoryExtendedImpl implements RewardsRepositoryExtended{
    private final ReactiveMongoTemplate mongoTemplate;

    public RewardsRepositoryExtendedImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Flux<Rewards> findByInitiativeIdWithBatch(String initiativeId, int batchSize) {
        Query query = Query.query(Criteria.where(Rewards.Fields.initiativeId).is(initiativeId)).cursorBatchSize(batchSize);
        return mongoTemplate.find(query, Rewards.class);
    }
}
