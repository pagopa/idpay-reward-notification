package it.gov.pagopa.reward.notification.service.rewards;

import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.enums.RewardStatus;
import it.gov.pagopa.reward.notification.model.Rewards;
import it.gov.pagopa.reward.notification.repository.RewardsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class RewardsServiceImpl implements RewardsService {
    private final RewardsRepository rewardsRepository;

    public RewardsServiceImpl(RewardsRepository rewardsRepository) {
        this.rewardsRepository = rewardsRepository;
    }

    @Override
    public Mono<RewardTransactionDTO> checkDuplicateReward(RewardTransactionDTO trx, String initiativeId) {
        return rewardsRepository.findById(trx.getId())
                .flatMap(result -> {
                    if(RewardStatus.ACCEPTED.equals(result.getStatus())){
                        log.info("[REWARD_NOTIFICATION][DUPLICATE_REWARD] Already processed reward {}", result.getId());
                        return Mono.<RewardTransactionDTO>error(new IllegalStateException("[REWARD_NOTIFICATION][DUPLICATE_REWARD] Already processed reward"));
                    } else {
                        return Mono.empty();
                    }
                })
                .defaultIfEmpty(trx)
                .onErrorResume(e -> Mono.empty())
                .doOnNext(x -> log.trace("[REWARD_NOTIFICATION] Duplicate check successful ended: {}_{}", trx.getId(), initiativeId));
    }

    @Override
    public Mono<Rewards> save(Rewards reward) {
        return rewardsRepository.save(reward);
    }
}
