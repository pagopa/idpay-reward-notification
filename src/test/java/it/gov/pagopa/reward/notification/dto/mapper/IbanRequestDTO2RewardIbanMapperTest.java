package it.gov.pagopa.reward.notification.dto.mapper;

import it.gov.pagopa.reward.notification.dto.iban.IbanRequestDTO;
import it.gov.pagopa.reward.notification.model.RewardIban;
import it.gov.pagopa.reward.notification.test.fakers.IbanRequestDTOFaker;
import it.gov.pagopa.reward.notification.test.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class IbanRequestDTO2RewardIbanMapperTest {
    @Test
    void apply(){
        // Given
        IbanRequestDTO2RewardIbanMapper ibanRequestDTO2RewardIbanMapper = new IbanRequestDTO2RewardIbanMapper();

        IbanRequestDTO ibanRequestDTO = IbanRequestDTOFaker.mockInstance(1);

        // When
        RewardIban result = ibanRequestDTO2RewardIbanMapper.apply(ibanRequestDTO);

        // Then
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result, "checkIbanOutcome");
        Assertions.assertEquals(ibanRequestDTO2RewardIbanMapper.buildId(ibanRequestDTO),result.getId());
        Assertions.assertEquals(ibanRequestDTO.getUserId(), result.getUserId());
        Assertions.assertEquals(ibanRequestDTO.getInitiativeId(), result.getInitiativeId());
        Assertions.assertEquals(ibanRequestDTO.getIban(), result.getIban());
    }
}