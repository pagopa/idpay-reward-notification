package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.dto.controller.FeedbackImportFilter;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class RewardOrganizationImportsRepositoryExtendedImpl implements RewardOrganizationImportsRepositoryExtended{
    public static final String FIELD_ORGANIZATION_ID = RewardOrganizationImport.Fields.organizationId;
    public static final String FIELD_INITIATIVE_ID = RewardOrganizationImport.Fields.initiativeId;
    public static final String FIELD_FILE_PATH = RewardOrganizationImport.Fields.filePath;
    public static final String FIELD_STATUS = RewardOrganizationImport.Fields.status;
    public static final String FIELD_ELAB_DATE = RewardOrganizationImport.Fields.elabDate;
    public static final String FIELD_ERRORS = RewardOrganizationImport.Fields.errors;

    private final ReactiveMongoTemplate mongoTemplate;

    public RewardOrganizationImportsRepositoryExtendedImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Flux<RewardOrganizationImport> findAllBy(String organizationId, String initiativeId, Pageable pageable, FeedbackImportFilter filters) {

        Query query = new Query().addCriteria(getCriteria(organizationId, initiativeId, filters));
        query.fields().exclude(FIELD_ERRORS);

        return mongoTemplate
                .find(
                        query.with(getPageable(pageable)),
                        RewardOrganizationImport.class
                ).doOnNext(r -> r.setErrors(null));

    }

    @Override
    public Mono<Long> countAll(String organizationId, String initiativeId, FeedbackImportFilter filters) {

        return mongoTemplate
                .count(
                        Query.query(getCriteria(organizationId, initiativeId, filters)),
                        RewardOrganizationImport.class
                );

    }

    @Override
    public Mono<RewardOrganizationImport> findByImportId(String organizationId, String initiativeId, String importId) {
        Criteria criteria = getCriteria(organizationId, initiativeId, null);
        criteria.andOperator(Criteria.where(FIELD_FILE_PATH).is(importId));

        return mongoTemplate
                .findOne(
                        Query.query(criteria),
                        RewardOrganizationImport.class
                );
    }

    private Criteria getCriteria(String organizationId, String initiativeId, FeedbackImportFilter filters) {
        Criteria criteria = Criteria
                .where(FIELD_ORGANIZATION_ID).is(organizationId)
                .and(FIELD_INITIATIVE_ID).is(initiativeId);

        // if filters are set, update the criteria; else, use default query
        updateCriteriaWithFilters(criteria, filters);
        return criteria;
    }

    private void updateCriteriaWithFilters(Criteria criteria, FeedbackImportFilter filters) {
        if (filters != null){
            List<Criteria> criteriaList = new ArrayList<>();

            // status
            if (filters.getStatus() != null) {
                criteriaList.add(Criteria.where(FIELD_STATUS).is(filters.getStatus()));
            }

            //notificationDate
            if (filters.getElabDateFrom() != null) {
                criteriaList.add(Criteria.where(FIELD_ELAB_DATE).gte(filters.getElabDateFrom()));
            }
            if (filters.getElabDateTo() != null) {
                criteriaList.add(Criteria.where(FIELD_ELAB_DATE).lte(filters.getElabDateTo()));
            }

            criteria.andOperator(criteriaList);
        }
    }

    private Pageable getPageable(Pageable pageable) {
        if (pageable == null) {
            pageable = Pageable.unpaged();
        }
        return pageable;
    }
}
