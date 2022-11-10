package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;

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
    public Flux<String> findInitiatives2Notify(Collection<String> initiativeIds2Exclude) {
        return mongoTemplate.findDistinct(
                Query.query(Criteria
                        .where(FIELD_STATUS).is(RewardNotificationStatus.TO_SEND)
                        .and(FIELD_INITIATIVE_ID).nin(initiativeIds2Exclude)
                        .and(FIELD_EXPORT_ID).isNull()
                        .andOperator(
                                Criteria.where(FIELD_NOTIFICATION_DATE).gte(LocalDate.now().minusDays(dayBeforeToSearch)),
                                Criteria.where(FIELD_NOTIFICATION_DATE).lte(LocalDate.now())
                        )
                ),
                FIELD_INITIATIVE_ID,
                RewardsNotification.class,
                String.class
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
}
