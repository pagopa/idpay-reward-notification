package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.dto.controller.ExportFilter;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.service.utils.ExportConstants;
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
    public Flux<RewardOrganizationExport> findAllBy(String organizationId, String initiativeId, Pageable pageable, ExportFilter filters) {

        Criteria criteria = Criteria
                .where(RewardOrganizationExport.Fields.organizationId).is(organizationId)
                .and(RewardOrganizationExport.Fields.initiativeId).is(initiativeId)
                .and(RewardOrganizationExport.Fields.status).in(ExportConstants.EXPORT_EXPOSED_STATUSES);
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

        Criteria criteria = Criteria
                .where(RewardOrganizationExport.Fields.organizationId).is(organizationId)
                .and(RewardOrganizationExport.Fields.initiativeId).is(initiativeId)
                .and(RewardOrganizationExport.Fields.status).in(ExportConstants.EXPORT_EXPOSED_STATUSES);
        // if filters are set, update the criteria; else, use default query
        updateCriteriaWithFilters(criteria, filters);

        return mongoTemplate
                .count(
                        Query.query(criteria).with(getPageable(pageable)),
                        RewardOrganizationExport.class
                );
    }

    private Pageable getPageable(Pageable pageable){
        if (pageable == null) {
            pageable = Pageable.unpaged();
        }
        return pageable;
    }

    private void updateCriteriaWithFilters(Criteria criteria, ExportFilter filters) {
        if (filters != null){
            List<Criteria> criteriaList = new ArrayList<>();

            // status
            if (filters.getStatus() != null) {
                criteriaList.add(Criteria.where(RewardOrganizationExport.Fields.status).is(filters.getStatus()));
            }

            //notificationDate
            if (filters.getNotificationDateFrom() != null && filters.getNotificationDateTo() != null) {
                criteriaList.add(Criteria.where(RewardOrganizationExport.Fields.notificationDate).gte(filters.getNotificationDateFrom()));
                criteriaList.add(Criteria.where(RewardOrganizationExport.Fields.notificationDate).lte(filters.getNotificationDateTo()));
            } else if (filters.getNotificationDateTo() != null) {
                criteriaList.add(Criteria.where(RewardOrganizationExport.Fields.notificationDate).gte(filters.getNotificationDateFrom()));
            } else if (filters.getNotificationDateFrom() != null) {
                criteriaList.add(Criteria.where(RewardOrganizationExport.Fields.notificationDate).lte(filters.getNotificationDateTo()));
            }

            criteria.andOperator(criteriaList);
        }
    }
}
