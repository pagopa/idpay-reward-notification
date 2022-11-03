package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.dto.controller.ExportFilter;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class RewardOrganizationExportsRepositoryImpl implements RewardOrganizationExportsRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public RewardOrganizationExportsRepositoryImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Flux<RewardOrganizationExport> findAllByOrganizationIdAndInitiativeId(String organizationId, String initiativeId) {
        return mongoTemplate
                .find(
                        Query.query(
                                Criteria.where(RewardOrganizationExport.Fields.organizationId).is(organizationId)
                                        .and(RewardOrganizationExport.Fields.initiativeId).is(initiativeId)
                        ), RewardOrganizationExport.class
                );
    }

    @Override
    public Flux<RewardOrganizationExport> findAllWithFilters(String organizationId, String initiativeId, ExportFilter filters) {
        // TODO Filters
        return mongoTemplate
                .find(
                        Query.query(
                                Criteria.where(RewardOrganizationExport.Fields.organizationId).is(organizationId)
                                        .and(RewardOrganizationExport.Fields.initiativeId).is(initiativeId)
                        ), RewardOrganizationExport.class
                );
    }

    @Override
    public Mono<Long> countAll(String organizationId, String initiativeId) {
        return mongoTemplate
                .count(
                        Query.query(
                                Criteria.where(RewardOrganizationExport.Fields.organizationId).is(organizationId)
                                        .and(RewardOrganizationExport.Fields.initiativeId).is(initiativeId)
                        ), RewardOrganizationExport.class
                );
    }

    @Override
    public Mono<Page<RewardOrganizationExport>> findAllPaged(String organizationId, String initiativeId) {
        // TODO return Page
        return null;
    }
}
