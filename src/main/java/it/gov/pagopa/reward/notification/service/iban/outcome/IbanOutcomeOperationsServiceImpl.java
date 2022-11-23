package it.gov.pagopa.reward.notification.service.iban.outcome;

import it.gov.pagopa.reward.notification.dto.iban.IbanOutcomeDTO;
import it.gov.pagopa.reward.notification.dto.mapper.IbanOutcomeDTO2RewardIbanMapper;
import it.gov.pagopa.reward.notification.model.RewardIban;
import it.gov.pagopa.reward.notification.service.iban.RewardIbanService;
import it.gov.pagopa.reward.notification.utils.IbanConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class IbanOutcomeOperationsServiceImpl implements IbanOutcomeOperationsService{
    private final RewardIbanService rewardIbanService;
    private final IbanOutcomeDTO2RewardIbanMapper ibanOutcomeDTO2RewardIbanMapper;

    public IbanOutcomeOperationsServiceImpl(RewardIbanService rewardIbanService, IbanOutcomeDTO2RewardIbanMapper ibanOutcomeDTO2RewardIbanMapper) {
        this.rewardIbanService = rewardIbanService;
        this.ibanOutcomeDTO2RewardIbanMapper = ibanOutcomeDTO2RewardIbanMapper;
    }

    @Override
    public Mono<RewardIban> execute(IbanOutcomeDTO ibanOutcomeDTO) {
        if (IbanConstants.STATUS_KO.equals(ibanOutcomeDTO.getStatus())) {
            return rewardIbanService.deleteIban(ibanOutcomeDTO);
        }
        return Mono.just(ibanOutcomeDTO2RewardIbanMapper.apply(ibanOutcomeDTO))
                        .flatMap(rewardIbanService::save);
    }
}
