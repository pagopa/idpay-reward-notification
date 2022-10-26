package it.gov.pagopa.reward.notification.service.rewards;

import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.model.Rewards;
import reactor.core.publisher.Mono;

public interface RewardsService {

    Mono<RewardTransactionDTO> checkDuplicateReward(RewardTransactionDTO trx, String initiativeId);
    Mono<Rewards> save(Rewards reward);
}
