package it.gov.pagopa.reward.notification.service.iban.outcome.filter;

import it.gov.pagopa.reward.notification.dto.iban.IbanOutcomeDTO;

import java.util.function.Predicate;

/**
 * Filter to skip {@link IbanOutcomeDTO}
 * */
public interface IbanOutcomeFilter extends Predicate<IbanOutcomeDTO> {
}