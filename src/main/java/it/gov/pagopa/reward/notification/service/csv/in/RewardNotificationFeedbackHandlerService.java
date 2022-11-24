package it.gov.pagopa.reward.notification.service.csv.in;

import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationImportCsvDto;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.service.csv.in.utils.RewardNotificationFeedbackHandlerOutcome;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface RewardNotificationFeedbackHandlerService {
    Mono<RewardNotificationFeedbackHandlerOutcome> evaluate(RewardNotificationImportCsvDto row, RewardOrganizationImport importRequest, Map<String, RewardOrganizationExport> exportCache);
}
