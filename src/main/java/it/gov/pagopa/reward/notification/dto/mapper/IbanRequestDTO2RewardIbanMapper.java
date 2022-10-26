package it.gov.pagopa.reward.notification.dto.mapper;

import it.gov.pagopa.reward.notification.dto.iban.IbanRequestDTO;
import it.gov.pagopa.reward.notification.model.RewardIban;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.function.Function;

@Service
public class IbanRequestDTO2RewardIbanMapper implements Function<IbanRequestDTO, RewardIban> {
    @Override
    public RewardIban apply(IbanRequestDTO ibanRequestDTO) {
        return RewardIban.builder()
                .id(buildId(ibanRequestDTO))
                .userId(ibanRequestDTO.getUserId())
                .initiativeId(ibanRequestDTO.getInitiativeId())
                .iban(ibanRequestDTO.getIban())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static String buildId(IbanRequestDTO ibanRequestDTO) {
        return ibanRequestDTO.getUserId()
                .concat(ibanRequestDTO.getInitiativeId());
    }
}