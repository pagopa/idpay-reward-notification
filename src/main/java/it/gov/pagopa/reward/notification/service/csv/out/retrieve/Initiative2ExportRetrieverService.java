package it.gov.pagopa.reward.notification.service.csv.out.retrieve;

import it.gov.pagopa.reward.notification.enums.RewardOrganizationExportStatus;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import reactor.core.publisher.Mono;

/** it will
 * <ol>
 *     <li>check if there is an export to be executed</li>
 *     <li>if not present, it will search for reward notification to be exported, filling the export collection</li>
 * </ol>
 * */
public interface Initiative2ExportRetrieverService {
    /** exports having status {@link RewardOrganizationExportStatus#IN_PROGRESS} and {@link RewardOrganizationExport#getNotificationDate()} < {@link RewardOrganizationExport#getExportDate()} */
    Mono<RewardOrganizationExport> retrieveStuckExecution();
    /** {@link RewardOrganizationExportStatus#TO_DO} {@link  RewardOrganizationExport} */
    Mono<RewardOrganizationExport> retrieve();

    Mono<RewardOrganizationExport> reserveNextSplitExport(RewardOrganizationExport baseExport, int splitNumber);
}
