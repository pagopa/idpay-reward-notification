package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.enums.RewardOrganizationExportStatus;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface RewardOrganizationExportsRepository extends ReactiveMongoRepository<RewardOrganizationExport, String>, RewardOrganizationExportsRepositoryExtended {

    List<RewardOrganizationExportStatus> PENDING_STATUSES = List.of(RewardOrganizationExportStatus.IN_PROGRESS, RewardOrganizationExportStatus.TO_DO);

    Flux<RewardOrganizationExport> findByStatusIn(Collection<RewardOrganizationExportStatus> statuses);
    Flux<RewardOrganizationExport> findByExportDate(LocalDate exportDate);
    Mono<RewardOrganizationExport> findByOrganizationIdAndInitiativeIdAndId(String organizationId, String initiativeId, String exportId);

    default Flux<RewardOrganizationExport> findPendingOrTodayExports(){
        return findByStatusIn(PENDING_STATUSES)
                .concatWith(findByExportDate(LocalDate.now()))
                .distinct(RewardOrganizationExport::getId);
    }
}
