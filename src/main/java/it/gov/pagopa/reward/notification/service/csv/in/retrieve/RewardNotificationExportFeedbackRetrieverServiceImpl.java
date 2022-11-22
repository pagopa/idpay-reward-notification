package it.gov.pagopa.reward.notification.service.csv.in.retrieve;

import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationImportCsvDto;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationExportStatus;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationImportResult;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationExportsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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

        return repository.updateCountersOnRewardFeedback(firstFeedback, deltaRewardCents, export)
                .map(x -> deltaRewardCents);
    }
}
