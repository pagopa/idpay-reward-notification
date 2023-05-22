package it.gov.pagopa.reward.notification.service.csv.out.retrieve;

import it.gov.pagopa.reward.notification.dto.merchant.MerchantDetailDTO;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import org.apache.commons.lang3.tuple.Pair;
import reactor.core.publisher.Mono;

public interface Merchant2NotifyRetrieverService {
    Mono<Pair<RewardsNotification, MerchantDetailDTO>> retrieve(RewardsNotification reward);
}
