package it.gov.pagopa.reward.notification.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.notification.dto.controller.ExportFilter;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationExportStatus;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.service.csv.in.utils.RewardNotificationFeedbackExportDelta;
import it.gov.pagopa.reward.notification.utils.ExportConstants;
import it.gov.pagopa.reward.notification.utils.Utils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RewardOrganizationExportsRepositoryExtendedImpl implements RewardOrganizationExportsRepositoryExtended {

    public static final String FIELD_ID = RewardOrganizationExport.Fields.id;
    public static final String FIELD_INITIATIVE_ID = RewardOrganizationExport.Fields.initiativeId;
    public static final String FIELD_EXPORT_DATE = RewardOrganizationExport.Fields.exportDate;
    public static final String FIELD_STATUS = RewardOrganizationExport.Fields.status;
    public static final String FIELD_PERCENTAGE_RESULTED = RewardOrganizationExport.Fields.percentageResulted;
    public static final String FIELD_PERCENTAGE_RESULTED_OK = RewardOrganizationExport.Fields.percentageResultedOk;
    public static final String FIELD_PERCENTAGE_RESULTS = RewardOrganizationExport.Fields.percentageResults;
    public static final List<RewardOrganizationExportStatus> EXPORT_RETRYABLE_STATES = List.of(RewardOrganizationExportStatus.IN_PROGRESS, RewardOrganizationExportStatus.ERROR);

    private final ReactiveMongoTemplate mongoTemplate;

    public RewardOrganizationExportsRepositoryExtendedImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Flux<RewardOrganizationExport> findAllBy(String organizationId, String initiativeId, Pageable pageable, ExportFilter filters) {

        if (filters != null && filters.getStatus() != null && checkStatusNotInExported(filters.getStatus())) {
            return Flux.empty();
        } else {
            return mongoTemplate
                    .find(
                            Query.query(getCriteria(organizationId, initiativeId, filters)).with(getPageable(pageable)),
                            RewardOrganizationExport.class
                    );
        }
    }


    @Override
    public Mono<Long> countAll(String organizationId, String initiativeId, ExportFilter filters) {

        if (filters != null && filters.getStatus() != null && checkStatusNotInExported(filters.getStatus())) {
            return Mono.just(0L);
        } else {
            return mongoTemplate
                    .count(
                            Query.query(getCriteria(organizationId, initiativeId, filters)),
                            RewardOrganizationExport.class
                    );
        }
    }

    private boolean checkStatusNotInExported(String status) {
        return !ExportConstants.EXPORT_EXPOSED_STATUSES.contains(RewardOrganizationExportStatus.valueOf(status));
    }


    private Criteria getCriteria(String organizationId, String initiativeId, ExportFilter filters) {
        Criteria criteria = Criteria
                .where(RewardOrganizationExport.Fields.organizationId).is(organizationId)
                .and(RewardOrganizationExport.Fields.initiativeId).is(initiativeId);

        // if filters are set, update the criteria; else, use default query
        updateCriteriaWithFilters(criteria, filters);
        return criteria;
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
        if (filters != null) {
            List<Criteria> criteriaList = new ArrayList<>();

            // status
            if (filters.getStatus() != null) {
                criteriaList.add(Criteria.where(RewardOrganizationExport.Fields.status).is(filters.getStatus()));
            } else {
                criteriaList.add(Criteria.where(RewardOrganizationExport.Fields.status).in(ExportConstants.EXPORT_EXPOSED_STATUSES));
            }

            //notificationDate
            if (filters.getNotificationDateFrom() != null) {
                criteriaList.add(Criteria.where(RewardOrganizationExport.Fields.notificationDate).gte(filters.getNotificationDateFrom()));
            }
            if (filters.getNotificationDateTo() != null) {
                criteriaList.add(Criteria.where(RewardOrganizationExport.Fields.notificationDate).lte(filters.getNotificationDateTo()));
            }

            criteria.andOperator(criteriaList);
        } else {
            criteria.and(RewardOrganizationExport.Fields.status).in(ExportConstants.EXPORT_EXPOSED_STATUSES);
        }
    }

    @Override
    public Mono<RewardOrganizationExport> reserveStuckExport() {
        return mongoTemplate.findAndModify(
                Query.query(Criteria
                        .where(FIELD_STATUS).in(EXPORT_RETRYABLE_STATES)
                        .and(FIELD_EXPORT_DATE).lt(LocalDate.now())
                ),
                new Update()
                        .set(FIELD_EXPORT_DATE, LocalDate.now()),
                FindAndModifyOptions.options().returnNew(true),
                RewardOrganizationExport.class
        );
    }

    @Override
    public Mono<RewardOrganizationExport> reserveExport() {
        return mongoTemplate.findAndModify(
                Query.query(Criteria.where(FIELD_STATUS).is(RewardOrganizationExportStatus.TO_DO)),
                new Update()
                        .set(FIELD_STATUS, RewardOrganizationExportStatus.IN_PROGRESS)
                        .set(FIELD_EXPORT_DATE, LocalDate.now()),
                FindAndModifyOptions.options().returnNew(true),
                RewardOrganizationExport.class
        );
    }

    @Override
    public Mono<RewardOrganizationExport> configureNewExport(RewardOrganizationExport newExport) {
        return mongoTemplate.upsert(
                Query.query(
                        Criteria.where(FIELD_INITIATIVE_ID).is(newExport.getInitiativeId())
                                .and(FIELD_STATUS).in(RewardOrganizationExportStatus.TO_DO, RewardOrganizationExportStatus.IN_PROGRESS)
                ),
                new Update()
                        .setOnInsert(RewardOrganizationExport.Fields.id, newExport.getId())
                        .setOnInsert(RewardOrganizationExport.Fields.filePath, newExport.getFilePath())
                        .setOnInsert(RewardOrganizationExport.Fields.initiativeId, newExport.getInitiativeId())
                        .setOnInsert(RewardOrganizationExport.Fields.initiativeName, newExport.getInitiativeName())
                        .setOnInsert(RewardOrganizationExport.Fields.organizationId, newExport.getOrganizationId())
                        .setOnInsert(RewardOrganizationExport.Fields.notificationDate, newExport.getNotificationDate())
                        .setOnInsert(RewardOrganizationExport.Fields.progressive, newExport.getProgressive())
                        .setOnInsert(RewardOrganizationExport.Fields.status, newExport.getStatus())

                        .setOnInsert(RewardOrganizationExport.Fields.rewardsExportedCents, newExport.getRewardsExportedCents())
                        .setOnInsert(RewardOrganizationExport.Fields.rewardsResultsCents, newExport.getRewardsResultsCents())

                        .setOnInsert(RewardOrganizationExport.Fields.rewardNotified, newExport.getRewardNotified())
                        .setOnInsert(RewardOrganizationExport.Fields.rewardsResulted, newExport.getRewardsResulted())
                        .setOnInsert(RewardOrganizationExport.Fields.rewardsResultedOk, newExport.getRewardsResultedOk())

                        .setOnInsert(RewardOrganizationExport.Fields.percentageResults, newExport.getPercentageResults())
                        .setOnInsert(RewardOrganizationExport.Fields.percentageResulted, newExport.getPercentageResulted())
                        .setOnInsert(RewardOrganizationExport.Fields.percentageResultedOk, newExport.getPercentageResultedOk())

                ,
                RewardOrganizationExport.class
        ).flatMap(r -> {
            if (r.getMatchedCount() > 0) {
                return Mono.empty();
            } else {
                return Mono.just(newExport);
            }
        });
    }

    @Override
    public Mono<UpdateResult> updateCounters(RewardNotificationFeedbackExportDelta exportDelta) {
        return updateCountersInner(exportDelta.getExportInc(), exportDelta.getExportDeltaReward(), exportDelta.getExportIncOk(), exportDelta.getExport());
    }

    public Mono<UpdateResult> updateCountersInner(long incCount, long incRewardCents, long incOkCount, RewardOrganizationExport export) {
        boolean reward2update = incRewardCents != 0L;
        boolean count2update = incCount != 0L;
        boolean countOk2update = incOkCount != 0L;

        if (reward2update || count2update || countOk2update) {
            Update increments = new Update();

            if (reward2update) {
                buildRewardIncrements(increments, incRewardCents, export);
            }

            if (count2update) {
                buildCountIncrements(increments, incCount, export);
            }

            if (countOk2update) {
                buildCountOkIncrements(increments, incOkCount, export);
            }

            return mongoTemplate.updateFirst(
                    Query.query(Criteria.where(FIELD_ID).is(export.getId())),
                    increments,
                    RewardOrganizationExport.class
            );
        } else {
            return Mono.just(UpdateResult.acknowledged(0, null, null));
        }
    }

    private void buildRewardIncrements(Update increments, long incRewardCents, RewardOrganizationExport export) {
        increments.inc(RewardOrganizationExport.Fields.rewardsResultsCents, incRewardCents)
                .inc(RewardOrganizationExport.Fields.percentageResults, Utils.calcPercentage(incRewardCents, export.getRewardsExportedCents()));
    }

    private void buildCountIncrements(Update increments, long incCount, RewardOrganizationExport export) {
        increments.inc(RewardOrganizationExport.Fields.rewardsResulted, incCount)
                .inc(RewardOrganizationExport.Fields.percentageResulted, Utils.calcPercentage(incCount, export.getRewardNotified()));
    }

    private void buildCountOkIncrements(Update increments, long incOkCount, RewardOrganizationExport export) {
        increments.inc(RewardOrganizationExport.Fields.rewardsResultedOk, incOkCount)
                .inc(RewardOrganizationExport.Fields.percentageResultedOk, Utils.calcPercentage(incOkCount, export.getRewardNotified()));
    }

    @Override
    public Mono<UpdateResult> updateStatus(RewardOrganizationExportStatus nextStatus, Long percentageResultedFix, Long percentageResultedOkFix, Long percentageResultsFix, RewardOrganizationExport export) {
        Update update = new Update();
        if (nextStatus != null) {
            update.set(FIELD_STATUS, nextStatus);
        }
        if (percentageResultedFix != null) {
            update.set(FIELD_PERCENTAGE_RESULTED, percentageResultedFix);
        }
        if (percentageResultedOkFix != null) {
            update.set(FIELD_PERCENTAGE_RESULTED_OK, percentageResultedOkFix);
        }
        if (percentageResultsFix != null) {
            update.set(FIELD_PERCENTAGE_RESULTS, percentageResultsFix);
        }
        return mongoTemplate.updateFirst(
                new Query(Criteria.where(FIELD_ID).is(export.getId())),
                update,
                RewardOrganizationExport.class);
    }
}
