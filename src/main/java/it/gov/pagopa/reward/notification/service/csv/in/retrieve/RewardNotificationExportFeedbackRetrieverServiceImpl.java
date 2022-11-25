package it.gov.pagopa.reward.notification.service.csv.in.retrieve;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationImportCsvDto;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationExportStatus;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationImportResult;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationExportsRepository;
import it.gov.pagopa.reward.notification.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RewardNotificationExportFeedbackRetrieverServiceImpl implements RewardNotificationExportFeedbackRetrieverService {

    private final RewardOrganizationExportsRepository repository;

    public RewardNotificationExportFeedbackRetrieverServiceImpl(RewardOrganizationExportsRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<RewardOrganizationExport> retrieve(RewardsNotification notification, RewardNotificationImportCsvDto row, RewardOrganizationImport importRequest, Map<String, RewardOrganizationExport> exportCache) {
        RewardOrganizationExport exportCached = exportCache.get(notification.getExportId());
        if (exportCached != null) {
            return Mono.just(exportCached);
        } else {
            return repository.findById(notification.getExportId())
                    .switchIfEmpty(Mono.defer(() -> {
                        log.error("[REWARD_NOTIFICATION_FEEDBACK] Cannot find export related to current feedback reward notification: rewardNotificationId {}, externalId {}, exportDate {}, exportId {}, rowNumber: {}, filePath {}", notification.getId(), row.getUniqueID(), notification.getExportDate(), notification.getExportId(), row.getRowNumber(), importRequest.getFilePath());
                        return repository.save(RewardOrganizationExport.builder()
                                .id(notification.getExportId())
                                .organizationId(importRequest.getOrganizationId())
                                .initiativeId(importRequest.getInitiativeId())
                                .exportDate(notification.getExportDate().toLocalDate())
                                .notificationDate(notification.getNotificationDate())
                                .rewardNotified(-1L)
                                .rewardsExportedCents(-1L)
                                .status(RewardOrganizationExportStatus.EXPORTED)
                                .build()
                        );
                    }))

                    .doOnNext(e -> {
                        log.debug("[REWARD_NOTIFICATION_FEEDBACK] Retrieved export to which current row belong to: rewardNotificationId {}, exportId {}, rowNumber: {}, filePath {}", notification.getId(), notification.getExportId(), row.getRowNumber(), importRequest.getFilePath());
                        exportCache.put(notification.getExportId(), e);
                    });
        }
    }

    @Override
    public Mono<Long> updateCounters(RewardsNotification notification, RewardOrganizationExport export) {
        boolean firstFeedback = notification.getFeedbackHistory().size() == 1;

        // is first feedback  or previous feedback was a KO
        boolean isFirstFeedbackOrPreviousWasKo = firstFeedback
                || !RewardOrganizationImportResult.OK.equals(notification.getFeedbackHistory().get(notification.getFeedbackHistory().size() - 2).getResult());

        long deltaRewardCents;
        if (RewardNotificationStatus.COMPLETED_OK.equals(notification.getStatus())) {
            deltaRewardCents = isFirstFeedbackOrPreviousWasKo
                    ? notification.getRewardCents()
                    : 0L;
        } else {
            deltaRewardCents = isFirstFeedbackOrPreviousWasKo
                    ? 0L
                    : -notification.getRewardCents();
        }

        log.debug("[REWARD_NOTIFICATION_FEEDBACK] Updating counters of export {} with reward {}; firstFeedback:{} and deltaRewardCents:{}", notification.getExportId(), notification.getId(), firstFeedback, deltaRewardCents);

        return repository.updateCountersOnRewardFeedback(firstFeedback, deltaRewardCents, export)
                .map(x -> deltaRewardCents);
    }

    @Override
    public Flux<UpdateResult> updateExportStatus(List<String> exportIds) {
        log.debug("[REWARD_NOTIFICATION_FEEDBACK] Updating statuses of involved exports {}", exportIds);

        return repository.findAllById(exportIds)
                .flatMap(e->{
                    Long percentageResultedFix = checkPercentageValue(e.getRewardsResulted(), e.getRewardNotified(), e.getPercentageResulted());
                    Long percentageResultedOkFix = checkPercentageValue(e.getRewardsResultedOk(), e.getRewardNotified(), e.getPercentageResultedOk());
                    Long percentageResultsFix = checkPercentageValue(e.getRewardsResultsCents(), e.getRewardsExportedCents(), e.getPercentageResults());

                    RewardOrganizationExportStatus nextStatus=null;
                    if(ObjectUtils.firstNonNull(percentageResultedFix, e.getPercentageResulted())>=100_00L){
                        nextStatus = RewardOrganizationExportStatus.COMPLETE;
                    } else if(RewardOrganizationExportStatus.EXPORTED.equals(e.getStatus())){
                        nextStatus=RewardOrganizationExportStatus.PARTIAL;
                    }

                    if(nextStatus!=null || percentageResultedFix!=null || percentageResultedOkFix!=null || percentageResultsFix!=null){
                        logUpdateStatusOp(e, percentageResultedFix, percentageResultedOkFix, percentageResultsFix, nextStatus);
                        return repository.updateStatus(nextStatus, percentageResultedFix, percentageResultedOkFix, percentageResultsFix, e);
                    } else {
                        return Mono.just(UpdateResult.acknowledged(0, null, null));
                    }
                });
    }

    private void logUpdateStatusOp(RewardOrganizationExport e, Long percentageResultedFix, Long percentageResultedOkFix, Long percentageResultsFix, RewardOrganizationExportStatus nextStatus) {
        log.info("[REWARD_NOTIFICATION_FEEDBACK] Updating status and percentage of involved export{}{}{}{}{}", e.getId()
                , nextStatus !=null? ". Status from %s to %s".formatted(e.getStatus(), nextStatus) : ""
                , percentageResultedFix !=null? ". PercentageResulted from %s to %s".formatted(e.getPercentageResulted() , percentageResultedFix) : ""
                , percentageResultedOkFix !=null? ". PercentageResultedOk from %s to %s".formatted(e.getPercentageResultedOk() , percentageResultedOkFix) : ""
                , percentageResultsFix !=null? ". PercentageResults from %s to %s".formatted(e.getPercentageResults() , percentageResultsFix) : ""
                );
    }

    /** It will recalculate the percentage, if different, it will return the fixed value. It could differ caused by rounding to 2 decimal */
    private Long checkPercentageValue(long actual, long total, long percentageStored) {
        long value = Utils.calcPercentage(actual, total);
        return value == percentageStored? null : value;
    }
}
