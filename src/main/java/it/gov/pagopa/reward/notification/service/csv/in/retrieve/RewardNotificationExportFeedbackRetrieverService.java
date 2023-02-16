package it.gov.pagopa.reward.notification.service.csv.in.retrieve;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationImportCsvDto;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.service.csv.in.utils.RewardNotificationFeedbackExportDelta;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface RewardNotificationExportFeedbackRetrieverService {
    Mono<RewardOrganizationExport> retrieve(RewardsNotification rewardsNotification, RewardNotificationImportCsvDto row, RewardOrganizationImport importRequest, Map<String, RewardOrganizationExport> exportCache);
    Mono<RewardNotificationFeedbackExportDelta> updateCounters(RewardsNotification notification, RewardOrganizationExport export);

    Flux<UpdateResult> updateExportStatus(List<String> exportIds);
}
