package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.Collection;

public class RewardsNotificationRepositoryExtendedImpl implements RewardsNotificationRepositoryExtended {

    public static final String FIELD_STATUS = RewardsNotification.Fields.status;
    public static final String FIELD_NOTIFICATION_DATE = RewardsNotification.Fields.notificationDate;
    public static final String FIELD_INITIATIVE_ID = RewardsNotification.Fields.initiativeId;

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
}
