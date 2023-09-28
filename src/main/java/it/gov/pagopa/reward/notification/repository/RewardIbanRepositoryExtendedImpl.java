package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.model.RewardIban;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Flux;


public class RewardIbanRepositoryExtendedImpl implements RewardIbanRepositoryExtended{
    private final ReactiveMongoTemplate mongoTemplate;

    public RewardIbanRepositoryExtendedImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Flux<RewardIban> findByInitiativeIdWithBatch(String initiativeId, int batchSize) {
        Query query = Query.query(Criteria.where(RewardIban.Fields.initiativeId).is(initiativeId)).cursorBatchSize(batchSize);
        return mongoTemplate.find(query, RewardIban.class);
    }
}
