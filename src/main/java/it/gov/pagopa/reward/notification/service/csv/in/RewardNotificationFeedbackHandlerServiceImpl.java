package it.gov.pagopa.reward.notification.service.csv.in;

import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationImportCsvDto;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationImportResult;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.service.csv.RewardNotificationNotifierService;
import it.gov.pagopa.reward.notification.service.csv.in.retrieve.RewardNotificationExportFeedbackRetrieverService;
import it.gov.pagopa.reward.notification.service.csv.in.retrieve.RewardNotificationFeedbackRetrieverService;
import it.gov.pagopa.reward.notification.service.csv.in.utils.FeedbackEvaluationException;
import it.gov.pagopa.reward.notification.service.csv.in.utils.RewardNotificationFeedbackExportDelta;
import it.gov.pagopa.reward.notification.service.csv.in.utils.RewardNotificationFeedbackHandlerOutcome;
import it.gov.pagopa.reward.notification.utils.RewardFeedbackConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
public class RewardNotificationFeedbackHandlerServiceImpl implements RewardNotificationFeedbackHandlerService {

    private final RewardNotificationFeedbackRetrieverService notificationFeedbackRetrieverService;
    private final RewardNotificationExportFeedbackRetrieverService exportFeedbackRetrieverService;
    private final RewardNotificationNotifierService notificationNotifierService;

    public RewardNotificationFeedbackHandlerServiceImpl(RewardNotificationFeedbackRetrieverService notificationFeedbackRetrieverService, RewardNotificationExportFeedbackRetrieverService exportFeedbackRetrieverService, RewardNotificationNotifierService notificationNotifierService) {
        this.notificationFeedbackRetrieverService = notificationFeedbackRetrieverService;
        this.exportFeedbackRetrieverService = exportFeedbackRetrieverService;
        this.notificationNotifierService = notificationNotifierService;
    }

    @Override
    public Mono<RewardNotificationFeedbackHandlerOutcome> evaluate(RewardNotificationImportCsvDto row, RewardOrganizationImport importRequest, Map<String, RewardOrganizationExport> exportCache) {
        RewardOrganizationImportResult rowResult = RewardOrganizationImportResult.fromValue(row.getResult());

        if(rowResult == null){
            log.error("[REWARD_NOTIFICATION_FEEDBACK] unexpected feedback result {} handling uniqueId {} notified at row {} of import {}", row.getResult(), row.getUniqueID(), row.getRowNumber(), importRequest.getFilePath());
            return Mono.just(new RewardNotificationFeedbackHandlerOutcome(null, new RewardOrganizationImport.RewardOrganizationImportError(row.getRowNumber(), RewardFeedbackConstants.ImportFeedbackRowErrors.INVALID_RESULT), null));
        }

        return notificationFeedbackRetrieverService.retrieve(row, importRequest)
                .flatMap(rn -> retrieveExportAndEvaluate(rn, row, rowResult, importRequest, exportCache))

                .onErrorResume(e -> {
                    RewardFeedbackConstants.ImportFeedbackRowErrors error;
                    if(e instanceof FeedbackEvaluationException feedbackEvaluationException){
                        error=feedbackEvaluationException.getError();
                    } else {
                        log.error("[REWARD_NOTIFICATION_FEEDBACK] Something gone wrong while handling uniqueId {} notified at row {} of import {}", row.getUniqueID(), row.getRowNumber(), importRequest.getFilePath(), e);
                        error = RewardFeedbackConstants.ImportFeedbackRowErrors.GENERIC_ERROR;
                    }
                    return Mono.just(new RewardNotificationFeedbackHandlerOutcome(rowResult, new RewardOrganizationImport.RewardOrganizationImportError(row.getRowNumber(), error), null));
                });
    }

    private Mono<RewardNotificationFeedbackHandlerOutcome> retrieveExportAndEvaluate(RewardsNotification notification, RewardNotificationImportCsvDto row, RewardOrganizationImportResult rowResult, RewardOrganizationImport importRequest, Map<String, RewardOrganizationExport> exportCache) {
        return exportFeedbackRetrieverService.retrieve(notification, row, importRequest, exportCache)
                .flatMap(e -> evaluate(notification, row, rowResult, importRequest, e));
    }

    private Mono<RewardNotificationFeedbackHandlerOutcome> evaluate(RewardsNotification notification, RewardNotificationImportCsvDto row, RewardOrganizationImportResult rowResult, RewardOrganizationImport importRequest, RewardOrganizationExport export) {
        return notificationFeedbackRetrieverService.updateFeedbackHistory(notification, row, rowResult, importRequest)
                .flatMap(toNotify -> {
                    Mono<RewardNotificationFeedbackHandlerOutcome> outcomeMono;
                    if (Boolean.TRUE.equals(toNotify)) {
                        outcomeMono = exportFeedbackRetrieverService.updateCounters(notification, export)
                                .flatMap(exportDelta -> notificationNotifierService.notify(notification, exportDelta.getExportDeltaReward())
                                        .map(x -> new RewardNotificationFeedbackHandlerOutcome(rowResult, null, exportDelta)));
                    } else {
                        outcomeMono = Mono.just(new RewardNotificationFeedbackHandlerOutcome(rowResult, null, new RewardNotificationFeedbackExportDelta(export.getId(), 0L, 0L, 0L)));
                    }

                    return outcomeMono;
                });
    }

}
