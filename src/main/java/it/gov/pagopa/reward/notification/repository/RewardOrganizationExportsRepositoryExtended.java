package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import reactor.core.publisher.Mono;

public interface RewardOrganizationExportsRepositoryExtended {
    /** new export on an initiative configured only if there are not pending ({@link it.gov.pagopa.reward.notification.enums.ExportStatus#TO_DO} or {@link it.gov.pagopa.reward.notification.enums.ExportStatus#IN_PROGRESS}) exports on that initiative */
    Mono<RewardOrganizationExport> configureNewExport(RewardOrganizationExport newExport);
    /** It will retrieve an export changing its status from {@link it.gov.pagopa.reward.notification.enums.ExportStatus#TO_DO} to {@link it.gov.pagopa.reward.notification.enums.ExportStatus#IN_PROGRESS} if any, setting {@link RewardOrganizationExport#getExportDate()} to today and allowing the current process to handle it */
    Mono<RewardOrganizationExport> reserveExport();
    /** It will retrieve an export having status {@link it.gov.pagopa.reward.notification.enums.ExportStatus#IN_PROGRESS} and {@link RewardOrganizationExport#getExportDate()} < today, updating it to today, and allowing the current process to handle it */
    Mono<RewardOrganizationExport> reserveStuckExport();
}
