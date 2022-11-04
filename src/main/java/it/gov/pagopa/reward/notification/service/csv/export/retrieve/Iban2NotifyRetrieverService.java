package it.gov.pagopa.reward.notification.service.csv.export.retrieve;

import it.gov.pagopa.reward.notification.model.RewardsNotification;
import reactor.core.publisher.Mono;

public interface Iban2NotifyRetrieverService {
    Mono<RewardsNotification> retrieveIban(RewardsNotification rewardsNotification);
}
