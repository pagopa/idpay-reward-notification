package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface RewardOrganizationExportsRepository extends ReactiveMongoRepository<RewardOrganizationExport, String>, RewardOrganizationExportsRepositoryExtended {
}
