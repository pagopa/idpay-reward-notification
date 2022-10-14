package it.gov.pagopa.reward.notification.service.iban.request;

import it.gov.pagopa.reward.notification.model.RewardIban;
import reactor.core.publisher.Mono;

public interface RewardIbanService {
    Mono<RewardIban> save(RewardIban rewardIban);
}