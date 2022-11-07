package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.dto.controller.ExportFilter;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class RewardOrganizationExportsRepositoryExtendedImpl implements RewardOrganizationExportsRepositoryExtended {

    private final ReactiveMongoTemplate mongoTemplate;

    public RewardOrganizationExportsRepositoryExtendedImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Flux<RewardOrganizationExport> findAll(String organizationId, String initiativeId, Pageable pageable, ExportFilter filters) {

        Criteria criteria = Criteria.where(RewardOrganizationExport.Fields.organizationId).is(organizationId)
                .and(RewardOrganizationExport.Fields.initiativeId).is(initiativeId);
        // if filters are set, update the criteria; else, use default query
        updateCriteriaWithFilters(criteria, filters);

        return mongoTemplate
                .find(
                        Query.query(criteria).with(getPageable(pageable)),
                        RewardOrganizationExport.class
                );
    }

    @Override
    public Mono<Long> countAll(String organizationId, String initiativeId, Pageable pageable, ExportFilter filters) {

        Criteria criteria = Criteria.where(RewardOrganizationExport.Fields.organizationId).is(organizationId)
                .and(RewardOrganizationExport.Fields.initiativeId).is(initiativeId);
        // if filters are set, update the criteria; else, use default query
        updateCriteriaWithFilters(criteria, filters);

        return mongoTemplate
                .count(
                        Query.query(criteria).with(getPageable(pageable)),
                        RewardOrganizationExport.class
                );
    }

    @Override
    public Mono<Page<RewardOrganizationExport>> findAllPaged(String organizationId, String initiativeId, Pageable pageable) {
        // TODO return Page
        return null;
    }

    private Pageable getPageable(Pageable pageable){
        if (pageable == null) {
            pageable = Pageable.unpaged();
        }
        return pageable;
    }

    private void updateCriteriaWithFilters(Criteria criteria, ExportFilter filters) {
        // status
        if (filters.getStatus() != null) {
            criteria.and(RewardOrganizationExport.Fields.status).is(filters.getStatus());
        }

        //notificationDate
        if (filters.getNotificationDateFrom() != null && filters.getNotificationDateTo() != null) {
            criteria.andOperator(
                    Criteria.where(RewardOrganizationExport.Fields.notificationDate).gte(filters.getNotificationDateFrom()),
                    Criteria.where(RewardOrganizationExport.Fields.notificationDate).lte(filters.getNotificationDateTo()));
        } else if (filters.getNotificationDateTo() != null) {
            criteria.and(RewardOrganizationExport.Fields.notificationDate).gte(filters.getNotificationDateFrom());
        } else if (filters.getNotificationDateFrom() != null) {
            criteria.and(RewardOrganizationExport.Fields.notificationDate).lte(filters.getNotificationDateTo());
        }
    }
}
