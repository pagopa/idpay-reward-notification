package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface RewardOrganizationExportsRepositoryExtended extends ReactiveMongoRepository<RewardOrganizationExport, String>, RewardOrganizationExportsRepository {
}
