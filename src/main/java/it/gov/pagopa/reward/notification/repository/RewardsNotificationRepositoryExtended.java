package it.gov.pagopa.reward.notification.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Collection;

public interface RewardsNotificationRepositoryExtended {
    Flux<String> findInitiatives2Notify(Collection<String> initiativeIds2Exclude);
    Flux<RewardsNotification> findRewards2Notify(String initiativeId, LocalDate notificationDate);
    Flux<RewardsNotification> findExportRewards(String exportId);

    Mono<UpdateResult> updateExportStatus(String rewardNotificationId, String iban, String checkIbanResult, String exportId);
}
