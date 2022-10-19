package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.model.RewardsNotification;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface RewardsNotificationRepository extends ReactiveMongoRepository<RewardsNotification, String> {
}
