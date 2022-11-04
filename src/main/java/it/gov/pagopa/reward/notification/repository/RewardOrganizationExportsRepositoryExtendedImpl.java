package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.enums.ExportStatus;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Mono;

public class RewardOrganizationExportsRepositoryExtendedImpl implements RewardOrganizationExportsRepositoryExtended{

    public static final String FIELD_STATUS = RewardOrganizationExport.Fields.status;
    public static final String FIELD_INITIATIVE_ID = RewardOrganizationExport.Fields.initiativeId;

    private final ReactiveMongoTemplate mongoTemplate;

    public RewardOrganizationExportsRepositoryExtendedImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<RewardOrganizationExport> reserveExport() {
        return mongoTemplate.findAndModify(
                Query.query(Criteria.where(FIELD_STATUS).is(ExportStatus.TODO)),
                new Update()
                        .set(FIELD_STATUS, ExportStatus.IN_PROGRESS),
                RewardOrganizationExport.class
        );
    }

    @Override
    public Mono<RewardOrganizationExport> configureNewExport(RewardOrganizationExport newExport) {
        return mongoTemplate.upsert(
                Query.query(
                        Criteria.where(FIELD_INITIATIVE_ID).is(newExport.getInitiativeId())
                                .and(FIELD_STATUS).in(ExportStatus.TODO, ExportStatus.IN_PROGRESS)
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
        ).flatMap(r->{
            if(r.getMatchedCount()>0) {
                return Mono.empty();
            }
            else {
                return Mono.just(newExport);
            }
        });
    }

}
