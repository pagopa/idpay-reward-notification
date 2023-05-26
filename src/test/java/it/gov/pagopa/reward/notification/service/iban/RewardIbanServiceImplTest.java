package it.gov.pagopa.reward.notification.service.iban;

import it.gov.pagopa.reward.notification.dto.iban.IbanOutcomeDTO;
import it.gov.pagopa.reward.notification.dto.mapper.IbanOutcomeDTO2RewardIbanMapper;
import it.gov.pagopa.reward.notification.model.RewardIban;
import it.gov.pagopa.reward.notification.repository.RewardIbanRepository;
import it.gov.pagopa.reward.notification.utils.IbanConstants;
import it.gov.pagopa.reward.notification.test.fakers.IbanOutcomeDTOFaker;
import it.gov.pagopa.common.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

class RewardIbanServiceImplTest {

    @Test
    void save() {
        // Given
        RewardIbanRepository rewardIbanRepositoryMock = Mockito.mock(RewardIbanRepository.class);
        RewardIbanService rewardIbanService = new RewardIbanServiceImpl(rewardIbanRepositoryMock);

        RewardIban rewardIban = RewardIban.builder()
                .id("USERIDINITIATIVEID")
                .userId("USERID")
                .initiativeId("INITIATIVEID")
                .iban("IBAN")
                .build();

        Mockito.when(rewardIbanRepositoryMock.save(Mockito.same(rewardIban))).thenAnswer(r -> Mono.just(r.getArguments()[0]));

        // When
        RewardIban result = rewardIbanService.save(rewardIban).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(rewardIban, result);
        Mockito.verify(rewardIbanRepositoryMock).save(Mockito.any());
    }

    @Test
    void deleteIban() {
        // Given
        RewardIbanRepository rewardIbanRepositoryMock = Mockito.mock(RewardIbanRepository.class);
        RewardIbanService rewardIbanService = new RewardIbanServiceImpl(rewardIbanRepositoryMock);

        IbanOutcomeDTO ibanOutcomeDTO1 = IbanOutcomeDTOFaker.mockInstanceBuilder(1)
                .status(IbanConstants.STATUS_KO)
                .build();
        IbanOutcomeDTO ibanOutcomeDTO2 = IbanOutcomeDTOFaker.mockInstanceBuilder(2)
                .status(IbanConstants.STATUS_KO)
                .build();

        String id1 = IbanOutcomeDTO2RewardIbanMapper.buildId(ibanOutcomeDTO1);
        String id2 = IbanOutcomeDTO2RewardIbanMapper.buildId(ibanOutcomeDTO2);

        RewardIban rewardIban1 = RewardIban.builder()
                .id(IbanOutcomeDTO2RewardIbanMapper.buildId(ibanOutcomeDTO1))
                .userId(ibanOutcomeDTO1.getUserId())
                .initiativeId(ibanOutcomeDTO1.getInitiativeId())
                .iban(ibanOutcomeDTO1.getIban())
                .timestamp(LocalDateTime.of(2022,10,14,17,23))
                .build();

        Mockito.when(rewardIbanRepositoryMock.deleteByIdAndIban(id1, ibanOutcomeDTO1.getIban())).thenReturn(Mono.just(rewardIban1));
        Mockito.when(rewardIbanRepositoryMock.deleteByIdAndIban(id2, ibanOutcomeDTO2.getIban())).thenReturn(Mono.empty());

        // When
        RewardIban result1 = rewardIbanService.deleteIban(ibanOutcomeDTO1).block();
        RewardIban result2 = rewardIbanService.deleteIban(ibanOutcomeDTO2).block();

        // Then
        Assertions.assertNotNull(result1);
        Assertions.assertEquals(ibanOutcomeDTO1.getUserId(), result1.getUserId());
        Assertions.assertEquals(ibanOutcomeDTO1.getInitiativeId(), result1.getInitiativeId());
        Assertions.assertEquals(ibanOutcomeDTO1.getIban(), result1.getIban());
        TestUtils.checkNotNullFields(result1, "checkIbanOutcome");

        Assertions.assertNull(result2);

        Mockito.verify(rewardIbanRepositoryMock, Mockito.times(2)).deleteByIdAndIban(Mockito.anyString(),Mockito.anyString());
    }
}