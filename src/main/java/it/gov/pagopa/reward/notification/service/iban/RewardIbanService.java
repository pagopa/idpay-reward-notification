package it.gov.pagopa.reward.notification.service.iban;

import it.gov.pagopa.reward.notification.dto.iban.IbanOutcomeDTO;
import it.gov.pagopa.reward.notification.model.RewardIban;
import reactor.core.publisher.Mono;

public interface RewardIbanService {
    Mono<RewardIban> save(RewardIban rewardIban);
    Mono<RewardIban> deleteIban(IbanOutcomeDTO ibanOutcomeDTO);
    Mono<RewardIban> updateStatus(IbanOutcomeDTO ibanOutcomeDTO);
}