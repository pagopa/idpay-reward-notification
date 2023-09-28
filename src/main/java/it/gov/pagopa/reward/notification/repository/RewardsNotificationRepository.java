package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface RewardsNotificationRepository extends ReactiveMongoRepository<RewardsNotification, String>, RewardsNotificationRepositoryExtended {
    Mono<Long> countByBeneficiaryIdAndInitiativeIdAndOrdinaryIdIsNull(String beneficiaryId, String initiativeId);
    Flux<RewardsNotification> findByBeneficiaryIdAndInitiativeIdAndNotificationDateAndStatusAndOrdinaryIdIsNull(String beneficiaryId, String initiativeId, LocalDate notificationDate, RewardNotificationStatus status);

    Flux<RewardsNotification> findByBeneficiaryIdAndInitiativeIdAndNotificationDateGreaterThanAndStatusAndOrdinaryIdIsNull(String beneficiaryId, String initiativeId, LocalDate notificationDate, RewardNotificationStatus status);

    Mono<RewardsNotification> findByExternalId(String externalId);

    Flux<RewardsNotification> findByInitiativeIdAndNotificationDate(String initiativeId, LocalDate notificationDate);
    Mono<RewardsNotification> findByExternalIdAndOrganizationIdAndInitiativeId(String externalId, String organizationId, String initiativeId);

    Flux<RewardsNotification> findByBeneficiaryIdAndInitiativeIdAndStatusAndRemedialIdNull(String beneficiaryId, String initiativeId, RewardNotificationStatus status);

    Flux<RewardsNotification> findByBeneficiaryIdAndInitiativeIdAndStatusAndRejectionReasonAndExportIdNull(
            String beneficiaryId,
            String initiativeId,
            RewardNotificationStatus status,
            String rejectionReason
    );

    Flux<RewardsNotification> findByBeneficiaryIdAndInitiativeIdAndStatus(String beneficiaryId, String initiativeId, RewardNotificationStatus status);
}
