package it.gov.pagopa.reward.notification.service.iban.request;

import it.gov.pagopa.reward.notification.model.RewardIban;
import it.gov.pagopa.reward.notification.repository.RewardIbanRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class RewardIbanServiceImpl implements RewardIbanService {

    private final RewardIbanRepository rewardIbanRepository;

    public RewardIbanServiceImpl(RewardIbanRepository rewardIbanRepository) {
        this.rewardIbanRepository = rewardIbanRepository;
    }

    @Override
    public Mono<RewardIban> save(RewardIban rewardIban) {
        return rewardIbanRepository.save(rewardIban);
    }
}