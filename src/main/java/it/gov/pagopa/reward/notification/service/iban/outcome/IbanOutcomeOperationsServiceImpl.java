package it.gov.pagopa.reward.notification.service.iban.outcome;

import it.gov.pagopa.reward.notification.dto.iban.IbanOutcomeDTO;
import it.gov.pagopa.reward.notification.model.RewardIban;
import it.gov.pagopa.reward.notification.service.iban.RewardIbanService;
import it.gov.pagopa.reward.notification.service.utils.IbanConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class IbanOutcomeOperationsServiceImpl implements IbanOutcomeOperationsService{
    private final RewardIbanService rewardIbanService;

    public IbanOutcomeOperationsServiceImpl(RewardIbanService rewardIbanService) {
        this.rewardIbanService = rewardIbanService;
    }

    @Override
    public Mono<RewardIban> execute(IbanOutcomeDTO ibanOutcomeDTO) {
        if (IbanConstants.STATUS_KO.equals(ibanOutcomeDTO.getStatus())) {
            return rewardIbanService.deleteIban(ibanOutcomeDTO);
        }
        return rewardIbanService.updateStatus(ibanOutcomeDTO);
    }
}
