package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.dto.controller.detail.ExportDetailFilter;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.utils.NotificationConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RewardsNotificationRepositoryExtendedImpl implements RewardsNotificationRepositoryExtended {

    public static final String FIELD_ID = RewardsNotification.Fields.id;
    public static final String FIELD_INITIATIVE_ID = RewardsNotification.Fields.initiativeId;
    public static final String FIELD_STATUS = RewardsNotification.Fields.status;
    public static final String FIELD_NOTIFICATION_DATE = RewardsNotification.Fields.notificationDate;
    public static final String FIELD_IBAN = RewardsNotification.Fields.iban;
    public static final String FIELD_CHECK_IBAN_RESULT = RewardsNotification.Fields.checkIbanResult;
    public static final String FIELD_EXPORT_DATE = RewardsNotification.Fields.exportDate;
    public static final String FIELD_EXPORT_ID = RewardsNotification.Fields.exportId;

    private final int dayBeforeToSearch;
    private final ReactiveMongoTemplate mongoTemplate;

    public RewardsNotificationRepositoryExtendedImpl(
            @Value("${app.csv.export.day-before}") int dayBeforeToSearch,
            ReactiveMongoTemplate mongoTemplate) {
        this.dayBeforeToSearch = dayBeforeToSearch;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Flux<String> findInitiatives2Notify(Collection<String> initiativeIds2Exclude, LocalDate notificationDateToSearch) {
        return mongoTemplate.findDistinct(
                Query.query(Criteria
                        .where(FIELD_STATUS).is(RewardNotificationStatus.TO_SEND)
                        .and(FIELD_INITIATIVE_ID).nin(initiativeIds2Exclude)
                        .and(FIELD_EXPORT_ID).isNull()
                        .andOperator(
                                Criteria.where(FIELD_NOTIFICATION_DATE).gte(notificationDateToSearch.minusDays(dayBeforeToSearch)),
                                Criteria.where(FIELD_NOTIFICATION_DATE).lte(notificationDateToSearch)
                        )
                ),
                FIELD_INITIATIVE_ID,
                RewardsNotification.class,
                String.class
        );
    }

    @Override
    public Flux<RewardsNotification> findNotificationsToReset(Collection<String> initiativeIds2Exclude, LocalDate notificationDateToSearch) {
        return mongoTemplate.find(
                Query.query(Criteria
                        .where(FIELD_INITIATIVE_ID).nin(initiativeIds2Exclude)
                        .and(FIELD_EXPORT_ID).ne(null)
                        .andOperator(
                                Criteria.where(FIELD_NOTIFICATION_DATE).gte(notificationDateToSearch.minusDays(dayBeforeToSearch)),
                                Criteria.where(FIELD_NOTIFICATION_DATE).lte(notificationDateToSearch)
                        )
                ),
                RewardsNotification.class
        );
    }

    @Override
    public Flux<RewardsNotification> findRewards2Notify(String initiativeId, LocalDate notificationDate) {
        return mongoTemplate.find(
                Query.query(Criteria
                        .where(FIELD_STATUS).is(RewardNotificationStatus.TO_SEND)
                        .and(FIELD_INITIATIVE_ID).is(initiativeId)
                        .and(FIELD_EXPORT_ID).isNull()
                        .andOperator(
                                Criteria.where(FIELD_NOTIFICATION_DATE).gte(notificationDate.minusDays(dayBeforeToSearch)),
                                Criteria.where(FIELD_NOTIFICATION_DATE).lte(notificationDate)
                        )
                ),
                RewardsNotification.class
        );
    }

    @Override
    public Flux<RewardsNotification> findExportRewards(String exportId) {
        return mongoTemplate.find(
                Query.query(Criteria
                        .where(FIELD_EXPORT_ID).is(exportId)),
                RewardsNotification.class
        );
    }

    @Override
    public Mono<String> updateExportStatus(String rewardNotificationId, String iban, String checkIbanResult, String exportId) {
        return mongoTemplate.updateFirst(
                Query.query(Criteria.where(FIELD_ID).is(rewardNotificationId)),
                new Update()
                        .set(FIELD_IBAN, iban)
                        .set(FIELD_CHECK_IBAN_RESULT, checkIbanResult)
                        .set(FIELD_STATUS, RewardNotificationStatus.EXPORTED)
                        .set(FIELD_EXPORT_DATE, LocalDateTime.now())
                        .set(FIELD_EXPORT_ID, exportId),
                RewardsNotification.class
        ).map(x->rewardNotificationId);
    }

    @Override
    public Mono<RewardsNotification> saveIfNotExists(RewardsNotification rewardsNotification) {
        return mongoTemplate.insert(rewardsNotification)
                .onErrorResume(DuplicateKeyException.class, e -> Mono.empty());
    }

    @Override
    public Flux<RewardsNotification> findAll(String exportId, String organizationId, String initiativeId, ExportDetailFilter filters, Pageable pageable) {
        if (filters != null && filters.getStatus() != null && checkStatusNotValid(filters.getStatus())) {
            return Flux.empty();
        } else {
            return mongoTemplate
                    .find(
                            Query.query(getCriteria(organizationId, initiativeId, exportId, filters)).with(getPageable(pageable)),
                            RewardsNotification.class
                    );
        }
    }

    @Override
    public Mono<Long> countAll(String exportId, String organizationId, String initiativeId, ExportDetailFilter filters) {
        if (filters != null && filters.getStatus() != null && checkStatusNotValid(filters.getStatus())) {
            return Mono.just(0L);
        } else {
            return mongoTemplate
                    .count(
                            Query.query(getCriteria(organizationId, initiativeId, exportId, filters)),
                            RewardsNotification.class
                    );
        }
    }

    private boolean checkStatusNotValid(String status) {
        return !NotificationConstants.REWARD_NOTIFICATION_EXPOSED_STATUS.contains(RewardNotificationStatus.valueOf(status));
    }

    private Criteria getCriteria(String organizationId, String initiativeId, String exportId, ExportDetailFilter filters) {
        Criteria criteria = Criteria
                .where(RewardsNotification.Fields.organizationId).is(organizationId)
                .and(RewardsNotification.Fields.initiativeId).is(initiativeId)
                .and(RewardsNotification.Fields.exportId).is(exportId);

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

    private void updateCriteriaWithFilters(Criteria criteria, ExportDetailFilter filters) {
        if (filters != null) {
            List<Criteria> criteriaList = new ArrayList<>();

            // status
            if (filters.getStatus() != null) {
                criteriaList.add(Criteria.where(RewardsNotification.Fields.status).is(filters.getStatus()));
            } else {
                criteriaList.add(Criteria.where(RewardsNotification.Fields.status).in(NotificationConstants.REWARD_NOTIFICATION_EXPOSED_STATUS));
            }

            //notificationDate
            if (filters.getCro() != null) {
                criteriaList.add(Criteria.where(RewardsNotification.Fields.cro).is(filters.getCro()));
            }

            criteria.andOperator(criteriaList);
        } else {
            criteria.and(RewardsNotification.Fields.status).in(NotificationConstants.REWARD_NOTIFICATION_EXPOSED_STATUS);
        }
    }
}
