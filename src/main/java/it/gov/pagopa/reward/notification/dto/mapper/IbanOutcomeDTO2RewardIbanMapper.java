package it.gov.pagopa.reward.notification.dto.mapper;

import it.gov.pagopa.reward.notification.dto.iban.IbanOutcomeDTO;
import it.gov.pagopa.reward.notification.model.RewardIban;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
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

    public static String buildId(IbanOutcomeDTO ibanOutcomeDTO) {
        return ibanOutcomeDTO.getUserId()
                .concat(ibanOutcomeDTO.getInitiativeId());
    }

    public static String buildId(RewardsNotification rewardsNotification) {
        return rewardsNotification.getUserId()
                .concat(rewardsNotification.getInitiativeId());
    }
}