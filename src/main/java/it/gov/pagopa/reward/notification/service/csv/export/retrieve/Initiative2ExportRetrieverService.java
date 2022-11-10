package it.gov.pagopa.reward.notification.service.csv.export.retrieve;

import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import reactor.core.publisher.Mono;

/** it will
 * <ol>
 *     <li>check if there is an export to be executed</li>
 *     <li>if not present, it will search for reward notification to be exported, filling the export collection</li>
 * </ol>
 * */
public interface Initiative2ExportRetrieverService {
    /** exports having status {@link it.gov.pagopa.reward.notification.enums.ExportStatus#IN_PROGRESS} and {@link RewardOrganizationExport#getNotificationDate()} < {@link RewardOrganizationExport#getExportDate()} */
    Mono<RewardOrganizationExport> retrieveStuckExecution();
    /** {@link it.gov.pagopa.reward.notification.enums.ExportStatus#TO_DO} {@link  RewardOrganizationExport} */
    Mono<RewardOrganizationExport> retrieve();

    Mono<RewardOrganizationExport> reserveNextSplitExport(RewardOrganizationExport baseExport, int splitNumber);
}
