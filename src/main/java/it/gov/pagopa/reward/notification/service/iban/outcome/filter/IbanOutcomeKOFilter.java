package it.gov.pagopa.reward.notification.service.iban.outcome.filter;

import it.gov.pagopa.reward.notification.dto.iban.IbanOutcomeDTO;
import it.gov.pagopa.reward.notification.service.utils.IbanConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Order(0)
public class IbanOutcomeKOFilter implements IbanOutcomeFilter{
    @Override
    public boolean test(IbanOutcomeDTO ibanOutcomeDTO) {
        boolean isKO = ibanOutcomeDTO.getStatus().equals(IbanConstants.STATUS_KO);
        if(!isKO){
            log.info("Status is not KO in {}", ibanOutcomeDTO);
        }
        return isKO;
    }
}