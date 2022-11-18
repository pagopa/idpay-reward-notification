package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import org.slf4j.Logger;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface RewardOrganizationImportsRepository extends ReactiveMongoRepository<RewardOrganizationImport, String>, RewardOrganizationImportsRepositoryExtended {
    Logger log = org.slf4j.LoggerFactory.getLogger(RewardOrganizationImportsRepository.class);

    default Mono<RewardOrganizationImport> createIfNotExistsOrReturnEmpty(RewardOrganizationImport entity) {
        return insert(entity)
                .onErrorResume(DuplicateKeyException.class, e-> {
                    log.info("Organization feedback import request already exists: {}", entity.getFilePath());
                    return Mono.empty();
                });
    }
}
