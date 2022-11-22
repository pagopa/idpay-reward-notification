package it.gov.pagopa.reward.notification.service.csv.in.retrieve;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationImportCsvDto;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

public interface RewardNotificationExportFeedbackRetrieverService {
    Mono<RewardOrganizationExport> retrieve(RewardsNotification rewardsNotification, RewardNotificationImportCsvDto row, RewardOrganizationImport importRequest, Map<String, RewardOrganizationExport> exportCache);
    Mono<UpdateResult> updateCounters(long incCount, BigDecimal incReward, long incOkCount, RewardOrganizationExport export);
}
