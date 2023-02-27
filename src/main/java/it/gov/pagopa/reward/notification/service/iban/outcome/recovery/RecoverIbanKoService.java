package it.gov.pagopa.reward.notification.service.iban.outcome.recovery;

import it.gov.pagopa.reward.notification.model.RewardIban;
import reactor.core.publisher.Mono;

public interface RecoverIbanKoService {

    Mono<RewardIban> recover(RewardIban rewardIban);
}
