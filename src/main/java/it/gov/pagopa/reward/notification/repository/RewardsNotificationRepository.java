package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface RewardsNotificationRepository extends ReactiveMongoRepository<RewardsNotification, String>, RewardsNotificationRepositoryExtended {
    Mono<Long> countByUserIdAndInitiativeIdAndOrdinaryIdIsNull(String userId, String initiativeId);
    Flux<RewardsNotification> findByUserIdAndInitiativeIdAndNotificationDateAndStatusAndOrdinaryIdIsNull(String userId, String initiativeId, LocalDate notificationDate, RewardNotificationStatus status);

    Flux<RewardsNotification> findByUserIdAndInitiativeIdAndNotificationDateGreaterThanAndStatusAndOrdinaryIdIsNull(String userId, String initiativeId, LocalDate notificationDate, RewardNotificationStatus status);

    Mono<RewardsNotification> findByExternalId(String externalId);

    Flux<RewardsNotification> findByInitiativeIdAndNotificationDate(String initiativeId, LocalDate notificationDate);

    Flux<RewardsNotification> findByUserIdAndInitiativeIdAndStatusAndRemedialIdNull(String userId, String initiativeId, RewardNotificationStatus status);

    Flux<RewardsNotification> findByUserIdAndInitiativeIdAndStatusAndRejectionReasonAndExportIdNull(
            String userId,
            String initiativeId,
            RewardNotificationStatus status,
            String rejectionReason
    );
}
