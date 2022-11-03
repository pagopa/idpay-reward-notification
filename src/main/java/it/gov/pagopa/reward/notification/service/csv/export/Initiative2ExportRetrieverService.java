package it.gov.pagopa.reward.notification.service.csv.export;

import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import reactor.core.publisher.Mono;

/** it will
 * <ol>
 *     <li>check if there is an export to be executed</li>
 *     <li>if not present, it will search for reward notification to be exported, filling the export collection</li>
 * </ol>
 * */
public interface Initiative2ExportRetrieverService {
    Mono<RewardOrganizationExport> retrieve();
}
