package it.gov.pagopa.reward.notification.service.iban.outcome.recovery;

import it.gov.pagopa.reward.notification.model.RewardIban;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import reactor.core.publisher.Mono;

import java.util.List;

public interface NeverExportedDiscardedRewardNotificationService {

    Mono<List<RewardsNotification>> handleNeverExportedDiscardedRewardNotification(RewardIban rewardIban);
}
