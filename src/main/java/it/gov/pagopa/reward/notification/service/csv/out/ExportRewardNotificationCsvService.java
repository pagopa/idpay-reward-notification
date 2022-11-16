package it.gov.pagopa.reward.notification.service.csv.out;

import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.service.csv.out.retrieve.Initiative2ExportRetrieverService;
import reactor.core.publisher.Flux;

public interface ExportRewardNotificationCsvService {
    /**
     * To verify if there are some initiative to be notified.
     * At most only a pending {@link  RewardOrganizationExport} will exists for each initiative.
     * A pending {@link  RewardOrganizationExport} has status {@link it.gov.pagopa.reward.notification.enums.ExportStatus#TO_DO} or
     * {@link it.gov.pagopa.reward.notification.enums.ExportStatus#IN_PROGRESS}.
     * Each day will be search:
     * <ol>
     *     <li>Stuck {@link  RewardOrganizationExport} (see {@link Initiative2ExportRetrieverService#retrieveStuckExecution()})</li>
     *     <li>New {@link RewardOrganizationExport} (see ${@link Initiative2ExportRetrieverService#retrieve()})</li>
     * </ol>
     * @see ExportInitiativeRewardsService
     * */
    Flux<RewardOrganizationExport> execute();
}
