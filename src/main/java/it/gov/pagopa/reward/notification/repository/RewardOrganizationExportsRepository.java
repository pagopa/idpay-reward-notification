package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface RewardOrganizationExportsRepository extends ReactiveMongoRepository<RewardOrganizationExport, String> {

    Flux<RewardOrganizationExport> findAllByOrganizationIdAndInitiativeId(String organizationId, String initiativeId);
}
