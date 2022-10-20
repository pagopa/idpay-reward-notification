package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.model.RewardsNotification;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

public interface RewardsNotificationRepository extends ReactiveMongoRepository<RewardsNotification, String> {
    Flux<RewardsNotification> findByUserIdAndInitiativeIdAndNotificationDate(String userId, String initiativeId, LocalDate notificationDate);
    Flux<RewardsNotification> findByUserIdAndInitiativeIdAndNotificationDateGreaterThan(String userId, String initiativeId, LocalDate notificationDate);
}
