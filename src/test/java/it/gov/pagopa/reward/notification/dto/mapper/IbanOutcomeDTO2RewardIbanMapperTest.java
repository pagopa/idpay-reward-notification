package it.gov.pagopa.reward.notification.dto.mapper;

import it.gov.pagopa.reward.notification.dto.iban.IbanOutcomeDTO;
import it.gov.pagopa.reward.notification.model.RewardIban;
import it.gov.pagopa.reward.notification.test.fakers.IbanOutcomeDTOFaker;
import it.gov.pagopa.common.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class IbanOutcomeDTO2RewardIbanMapperTest {
    @Test
    void apply(){
        // Given
        IbanOutcomeDTO2RewardIbanMapper ibanOutcomeDTO2RewardIbanMapper = new IbanOutcomeDTO2RewardIbanMapper();

        IbanOutcomeDTO ibanOutcomeDTO = IbanOutcomeDTOFaker.mockInstance(1);

        // When
        RewardIban result = ibanOutcomeDTO2RewardIbanMapper.apply(ibanOutcomeDTO);

        // Then
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result);

        Assertions.assertEquals(IbanOutcomeDTO2RewardIbanMapper.buildId(ibanOutcomeDTO),result.getId());
        Assertions.assertEquals(ibanOutcomeDTO.getUserId(), result.getUserId());
        Assertions.assertEquals(ibanOutcomeDTO.getInitiativeId(), result.getInitiativeId());
        Assertions.assertEquals(ibanOutcomeDTO.getIban(), result.getIban());
        Assertions.assertEquals(ibanOutcomeDTO.getStatus(),result.getCheckIbanOutcome());
    }
}