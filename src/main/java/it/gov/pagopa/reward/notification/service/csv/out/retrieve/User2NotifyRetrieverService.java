package it.gov.pagopa.reward.notification.service.csv.out.retrieve;

import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.model.User;
import org.apache.commons.lang3.tuple.Pair;
import reactor.core.publisher.Mono;

public interface User2NotifyRetrieverService {
    Mono<Pair<RewardsNotification, User>> retrieveUser(RewardsNotification rewardsNotification);
}
