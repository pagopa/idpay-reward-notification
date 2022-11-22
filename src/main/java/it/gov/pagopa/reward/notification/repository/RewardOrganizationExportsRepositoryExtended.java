package it.gov.pagopa.reward.notification.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.notification.dto.controller.ExportFilter;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationExportStatus;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RewardOrganizationExportsRepositoryExtended {

    Flux<RewardOrganizationExport> findAllBy(String organizationId, String initiativeId, Pageable pageable, ExportFilter filters);
    Mono<Long> countAll(String organizationId, String initiativeId, ExportFilter filters);

    /** new export on an initiative configured only if there are not pending ({@link RewardOrganizationExportStatus#TO_DO} or {@link RewardOrganizationExportStatus#IN_PROGRESS}) exports on that initiative */
    Mono<RewardOrganizationExport> configureNewExport(RewardOrganizationExport newExport);
    /** It will retrieve an export changing its status from {@link RewardOrganizationExportStatus#TO_DO} to {@link RewardOrganizationExportStatus#IN_PROGRESS} if any, setting {@link RewardOrganizationExport#getExportDate()} to today and allowing the current process to handle it */
    Mono<RewardOrganizationExport> reserveExport();
    /** It will retrieve an export having status {@link RewardOrganizationExportStatus#IN_PROGRESS} and {@link RewardOrganizationExport#getExportDate()} < today, updating it to today, and allowing the current process to handle it */
    Mono<RewardOrganizationExport> reserveStuckExport();

    Mono<UpdateResult> updateCountersOnRewardFeedback(boolean firstFeedback, long deltaRewardCents, RewardOrganizationExport export);
    Mono<UpdateResult> updateCounters(long incCount, long incRewardCents, long incOkCount, RewardOrganizationExport export);

    Mono<UpdateResult> updateStatus(RewardOrganizationExportStatus nextStatus, RewardOrganizationExport export);
}
