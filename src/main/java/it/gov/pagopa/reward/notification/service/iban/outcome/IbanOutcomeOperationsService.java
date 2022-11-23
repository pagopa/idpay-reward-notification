package it.gov.pagopa.reward.notification.service.iban.outcome;

import it.gov.pagopa.reward.notification.dto.iban.IbanOutcomeDTO;
import it.gov.pagopa.reward.notification.model.RewardIban;
import reactor.core.publisher.Mono;

/**
 * This component given an iban will and delete it inside DB
 * */
public interface IbanOutcomeOperationsService {
    Mono<RewardIban> execute(IbanOutcomeDTO ibanOutcomeDTO);
}
