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
        return switch (ibanOutcomeDTO.getStatus()){
            case IbanConstants.STATUS_KO -> rewardIbanService.deleteIban(ibanOutcomeDTO);
            case IbanConstants.STATUS_UNKNOWN_PSP -> rewardIbanService.updateStatus(ibanOutcomeDTO);
            default -> invalidStatusType(ibanOutcomeDTO);
        };
    }

    private Mono<RewardIban> invalidStatusType(IbanOutcomeDTO ibanOutcomeDTO){
        log.error("Error in evaluate iban %s. Cause: unexpected status type".formatted(ibanOutcomeDTO.getIban()));
        return Mono.empty();
    }
}
