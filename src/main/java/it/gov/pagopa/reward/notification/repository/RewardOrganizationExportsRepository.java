package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.enums.RewardOrganizationExportStatus;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface RewardOrganizationExportsRepository extends ReactiveMongoRepository<RewardOrganizationExport, String>, RewardOrganizationExportsRepositoryExtended {

    List<RewardOrganizationExportStatus> PENDING_STATUSES = List.of(RewardOrganizationExportStatus.IN_PROGRESS, RewardOrganizationExportStatus.TO_DO);

    Flux<RewardOrganizationExport> findByStatusIn(Collection<RewardOrganizationExportStatus> statuses);
    Flux<RewardOrganizationExport> findByNotificationDate(LocalDate notificationDate);

    default Flux<RewardOrganizationExport> findPendingAndTodayExports(){
        return findByStatusIn(PENDING_STATUSES)
                .concatWith(findByNotificationDate(LocalDate.now()))
                .distinct(RewardOrganizationExport::getId);
    }
}
