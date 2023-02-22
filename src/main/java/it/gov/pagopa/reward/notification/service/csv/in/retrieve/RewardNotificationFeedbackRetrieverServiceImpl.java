package it.gov.pagopa.reward.notification.service.csv.in.retrieve;

import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationImportCsvDto;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationImportResult;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.csv.in.utils.FeedbackEvaluationException;
import it.gov.pagopa.reward.notification.utils.RewardFeedbackConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
public class RewardNotificationFeedbackRetrieverServiceImpl implements RewardNotificationFeedbackRetrieverService {

    private final RewardsNotificationRepository repository;

    public RewardNotificationFeedbackRetrieverServiceImpl(RewardsNotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<RewardsNotification> retrieve(RewardNotificationImportCsvDto row, RewardOrganizationImport importRequest) {
        return repository.findByExternalId(row.getUniqueID())
                .doOnNext(rn -> {
                    log.debug("[REWARD_NOTIFICATION_FEEDBACK] Retrieved reward notification to which current row belong to: rewardNotificationId {}, exportId {}, rowNumber: {}, filePath {}", rn.getId(), rn.getExportId(), row.getRowNumber(), importRequest.getFilePath());

                    if (!importRequest.getInitiativeId().equals(rn.getInitiativeId())) {
                        log.info("[REWARD_NOTIFICATION_FEEDBACK] provided feedback for an unexpected initiative {} at row {} of import {}", rn.getInitiativeId(), row.getRowNumber(), importRequest.getFilePath());
                        throw new FeedbackEvaluationException(RewardFeedbackConstants.ImportFeedbackRowErrors.NOT_FOUND);
                    }

                    if (!importRequest.getOrganizationId().equals(rn.getOrganizationId())) {
                        log.info("[REWARD_NOTIFICATION_FEEDBACK] provided feedback for an unexpected organization {} at row {} of import {}", rn.getOrganizationId(), row.getRowNumber(), importRequest.getFilePath());
                        throw new FeedbackEvaluationException(RewardFeedbackConstants.ImportFeedbackRowErrors.NOT_FOUND);
                    }
                })

                .switchIfEmpty(Mono.defer(() -> {
                    log.error("[REWARD_NOTIFICATION_FEEDBACK] Cannot find rewardNotification having externalId {} notified at row {} of file {}", row.getUniqueID(), row.getRowNumber(), importRequest.getFilePath());
                    return Mono.error(new FeedbackEvaluationException(RewardFeedbackConstants.ImportFeedbackRowErrors.NOT_FOUND));
                }));
    }

    @Override
    public Mono<Boolean> updateFeedbackHistory(RewardsNotification notification, RewardNotificationImportCsvDto row, RewardOrganizationImportResult rowResult, RewardOrganizationImport importRequest) {
        int nextFeedbackIndex = 0;
        for (RewardsNotification.RewardNotificationHistory h : notification.getFeedbackHistory()) {
            if (h.getFeedbackFilePath().equals(importRequest.getFilePath())) {
                log.debug("[REWARD_NOTIFICATION_FEEDBACK] Feedback already elaborated: rewardNotificationId {}, exportId {}, rowNumber {}, filePath {}", notification.getId(), notification.getExportId(), row.getRowNumber(), importRequest.getFilePath());
                return Mono.just(false);
            } else if (h.getFeedbackDate().isAfter(importRequest.getFeedbackDate())) {
                break;
            }
            nextFeedbackIndex++;
        }

        RewardsNotification.RewardNotificationHistory history = RewardsNotification.RewardNotificationHistory.fromImportRow(row, rowResult, importRequest);

        boolean newestFeedback = nextFeedbackIndex == notification.getFeedbackHistory().size();
        if (newestFeedback) {
            notification.getFeedbackHistory().add(history);

            notification.setFeedbackElaborationDate(LocalDateTime.now());
            notification.setFeedbackDate(importRequest.getFeedbackDate());
            notification.setExecutionDate(row.getExecutionDate());
            notification.setCro(row.getCro());
            notification.setStatus(rowResult.toRewardNotificationStatus());
            notification.setResultCode(row.getResult());
            notification.setRejectionReason(row.getRejectionReason());
        } else {
            notification.getFeedbackHistory().add(nextFeedbackIndex, history);
        }

        log.debug("[REWARD_NOTIFICATION_FEEDBACK] storing feedback on rewardNotificationId {}, exportId {}, rowNumber {}, rowStatus {}, filePath {}, toNotify {}", notification.getId(), notification.getExportId(), row.getRowNumber(), rowResult, importRequest.getFilePath(), newestFeedback);

        return repository.save(notification)
                .map(x -> newestFeedback);
    }
}
