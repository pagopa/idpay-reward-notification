package it.gov.pagopa.reward.notification.service.csv.in.retrieve;

import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationImportCsvDto;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationImportResult;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import reactor.core.publisher.Mono;

public interface RewardNotificationFeedbackRetrieverService {
    Mono<RewardsNotification> retrieve(RewardNotificationImportCsvDto row, RewardOrganizationImport importRequest);
    /** It will update the {@link RewardsNotification} based the current row and return a boolean to inform about the need to notify the result */
    Mono<Boolean> updateFeedbackHistory(RewardsNotification notification, RewardNotificationImportCsvDto row, RewardOrganizationImportResult rowResult, RewardOrganizationImport importRequest);
}
