package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.dto.controller.detail.ExportDetailFilter;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Collection;

public interface RewardsNotificationRepositoryExtended {
    Flux<String> findInitiatives2Notify(Collection<String> initiativeIds2Exclude, LocalDate notificationDateToSearch);
    Flux<RewardsNotification> findRewards2Notify(String initiativeId, LocalDate notificationDate);
    Flux<RewardsNotification> findExportRewards(String exportId);

    Mono<String> updateExportStatus(String rewardNotificationId, String iban, String checkIbanResult, String exportId);

    Mono<RewardsNotification> saveIfNotExists(RewardsNotification rewardsNotification);

    Flux<RewardsNotification> findAll(String exportId, String organizationId, String initiativeId, ExportDetailFilter filters, Pageable pageable);
    Mono<Long> countAll(String exportId, String organizationId, String initiativeId, ExportDetailFilter filters);
}
