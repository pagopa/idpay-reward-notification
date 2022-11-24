package it.gov.pagopa.reward.notification.service.csv.out;

import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import reactor.core.publisher.Flux;

public interface ExportInitiativeRewardsService {
    /**
     * To build a CSV, compressing and uploading it to Azure Storage based on an {@link RewardOrganizationExport}.
     * In case of stuck export, it will search previous rows searching by {@link RewardsNotification#getExportId()},
     * then it will search for not notified ({@link it.gov.pagopa.reward.notification.enums.RewardNotificationStatus#TO_SEND} {@link RewardsNotification}) rewards
     * having {@link RewardsNotification#getNotificationDate()} <= {@link RewardOrganizationExport#getNotificationDate()} ()}
     * */
    Flux<RewardOrganizationExport> performExport(RewardOrganizationExport export, boolean isStuckExport);
}
