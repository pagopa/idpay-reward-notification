package it.gov.pagopa.reward.notification.dto.mapper;

import it.gov.pagopa.reward.notification.dto.iban.IbanOutcomeDTO;
import it.gov.pagopa.reward.notification.dto.iban.IbanRequestDTO;
import it.gov.pagopa.reward.notification.model.RewardIban;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.function.Function;

@Service
public class IbanOutcomeDTO2RewardIbanMapper implements Function<IbanOutcomeDTO, RewardIban> {
    @Override
    public RewardIban apply(IbanOutcomeDTO ibanOutcomeDTO) {
        return RewardIban.builder()
                .id(buildId(ibanOutcomeDTO))
                .userId(ibanOutcomeDTO.getUserId())
                .initiativeId(ibanOutcomeDTO.getInitiativeId())
                .iban(ibanOutcomeDTO.getIban())
                .timestamp(LocalDateTime.now())
                .checkIbanOutcome(ibanOutcomeDTO.getStatus())
                .build();
    }
    public String buildId(IbanRequestDTO ibanRequestDTO) {
        return ibanRequestDTO.getUserId()
                .concat(ibanRequestDTO.getInitiativeId());
    }
}