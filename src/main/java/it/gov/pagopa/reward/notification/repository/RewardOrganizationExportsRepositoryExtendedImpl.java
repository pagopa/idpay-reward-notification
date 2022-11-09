package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.dto.controller.ExportFilter;
import it.gov.pagopa.reward.notification.enums.ExportStatus;
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
                .and(RewardOrganizationExport.Fields.initiativeId).is(initiativeId);

        // if filters are set, update the criteria; else, use default query
        updateCriteriaWithFilters(criteria, filters);

        if (filters != null && filters.getStatus() != null && checkStatusNotInExported(filters.getStatus())) {
            return Flux.empty();
        } else {
            return mongoTemplate
                    .find(
                            Query.query(criteria).with(getPageable(pageable)),
                            RewardOrganizationExport.class
                    );
        }
    }

    @Override
    public Mono<Long> countAll(String organizationId, String initiativeId, Pageable pageable, ExportFilter filters) {

        Criteria criteria = Criteria
                .where(RewardOrganizationExport.Fields.organizationId).is(organizationId)
                .and(RewardOrganizationExport.Fields.initiativeId).is(initiativeId);

        // if filters are set, update the criteria; else, use default query
        updateCriteriaWithFilters(criteria, filters);

        if (filters != null && filters.getStatus() != null && checkStatusNotInExported(filters.getStatus())) {
            return Mono.empty();
        } else {
            return mongoTemplate
                    .count(
                            Query.query(criteria).with(getPageable(pageable)),
                            RewardOrganizationExport.class
                    );
        }
    }

    private boolean checkStatusNotInExported(String status) {
        return !ExportConstants.EXPORT_EXPOSED_STATUSES.contains(ExportStatus.valueOf(status));
    }

    private Pageable getPageable(Pageable pageable) {
        if (pageable == null) {
            pageable = Pageable.unpaged();
        }
        return pageable;
    }

    /**
     * Checks if filters are defined in the request. If they are, it checks which are requested and adds them to query;
     * else, it adds the default filter that wants the {@link RewardOrganizationExport}'s status to be included in the
     * {@link ExportConstants#EXPORT_EXPOSED_STATUSES} collection.
     */
    private void updateCriteriaWithFilters(Criteria criteria, ExportFilter filters) {
        if (filters != null){
            List<Criteria> criteriaList = new ArrayList<>();

            // status
            if (filters.getStatus() != null) {
                criteriaList.add(Criteria.where(RewardOrganizationExport.Fields.status).is(filters.getStatus()));
            } else {
                criteriaList.add(Criteria.where(RewardOrganizationExport.Fields.status).in(ExportConstants.EXPORT_EXPOSED_STATUSES));
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
        } else {
            criteria.and(RewardOrganizationExport.Fields.status).in(ExportConstants.EXPORT_EXPOSED_STATUSES);
        }
    }
}
