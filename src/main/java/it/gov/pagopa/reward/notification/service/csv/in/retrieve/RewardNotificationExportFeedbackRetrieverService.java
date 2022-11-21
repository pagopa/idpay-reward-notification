package it.gov.pagopa.reward.notification.service.csv.in.retrieve;

import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationImportCsvDto;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface RewardNotificationExportFeedbackRetrieverService {
    Mono<RewardOrganizationExport> retrieve(RewardsNotification rewardsNotification, RewardNotificationImportCsvDto row, RewardOrganizationImport importRequest, Map<String, RewardOrganizationExport> exportCache);
}
