package it.gov.pagopa.reward.notification.service.iban.outcome;

import it.gov.pagopa.reward.notification.dto.iban.IbanOutcomeDTO;
import it.gov.pagopa.reward.notification.dto.mapper.IbanRequestDTO2RewardIbanMapper;
import it.gov.pagopa.reward.notification.model.RewardIban;
import it.gov.pagopa.reward.notification.service.iban.RewardIbanService;
import it.gov.pagopa.reward.notification.service.utils.IbanConstants;
import it.gov.pagopa.reward.notification.test.fakers.IbanOutcomeDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class IbanOutcomeOperationsServiceImplTest {
    @Mock
    private RewardIbanService rewardIbanServiceMock;
    private IbanOutcomeOperationsService ibanOutcomeOperationsService;

    @BeforeEach
    void setUp() {
        ibanOutcomeOperationsService = new IbanOutcomeOperationsServiceImpl(rewardIbanServiceMock);
    }

    @Test
    void executeStatusKO() {
        // Given
        IbanOutcomeDTO ibanOutcomeDTO = IbanOutcomeDTOFaker.mockInstance(1);
        ibanOutcomeDTO.setStatus(IbanConstants.STATUS_KO);

        Mockito.when(rewardIbanServiceMock.deleteIban(ibanOutcomeDTO)).thenReturn(Mono.empty());

        // When
         Optional<RewardIban> result = ibanOutcomeOperationsService.execute(ibanOutcomeDTO).blockOptional();

        // Then
        Assertions.assertTrue(result.isEmpty());

        Mockito.verify(rewardIbanServiceMock).deleteIban(ibanOutcomeDTO);
        Mockito.verify(rewardIbanServiceMock, Mockito.never()).updateStatus(Mockito.any());
    }

    @Test
    void executeNotStatusKO() {
        // Given
        IbanOutcomeDTO ibanOutcomeDTO = IbanOutcomeDTOFaker.mockInstance(1);
        ibanOutcomeDTO.setStatus(IbanConstants.STATUS_UNKNOWN_PSP);

        RewardIban rewardIban = RewardIban.builder()
                .id(IbanRequestDTO2RewardIbanMapper.buildId(ibanOutcomeDTO))
                .userId(ibanOutcomeDTO.getUserId())
                .initiativeId(ibanOutcomeDTO.getInitiativeId())
                .iban(ibanOutcomeDTO.getIban())
                .checkIbanOutcome(ibanOutcomeDTO.getStatus())
                .timestamp(LocalDateTime.now()).build();

        Mockito.when(rewardIbanServiceMock.updateStatus(ibanOutcomeDTO)).thenReturn(Mono.just(rewardIban));

        // When
        RewardIban result = ibanOutcomeOperationsService.execute(ibanOutcomeDTO).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(rewardIban, result);

        Mockito.verify(rewardIbanServiceMock).updateStatus(ibanOutcomeDTO);
        Mockito.verify(rewardIbanServiceMock, Mockito.never()).deleteIban(Mockito.any());
    }
}