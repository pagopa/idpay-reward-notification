package it.gov.pagopa.reward.notification.service.iban.outcome;

import it.gov.pagopa.reward.notification.dto.iban.IbanOutcomeDTO;
import it.gov.pagopa.reward.notification.dto.mapper.IbanOutcomeDTO2RewardIbanMapper;
import it.gov.pagopa.reward.notification.model.RewardIban;
import it.gov.pagopa.reward.notification.service.iban.RewardIbanService;
import it.gov.pagopa.reward.notification.utils.IbanConstants;
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
    @Mock
    private IbanOutcomeDTO2RewardIbanMapper ibanOutcomeDTO2RewardIbanMapperMock;
    private IbanOutcomeOperationsService ibanOutcomeOperationsService;

    @BeforeEach
    void setUp() {
        ibanOutcomeOperationsService = new IbanOutcomeOperationsServiceImpl(rewardIbanServiceMock, ibanOutcomeDTO2RewardIbanMapperMock);
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
        Mockito.verify(rewardIbanServiceMock, Mockito.never()).save(Mockito.any());
    }

    @Test
    void executeNotStatusKO() {
        // Given
        IbanOutcomeDTO ibanOutcomeUnknownDTO = IbanOutcomeDTOFaker.mockInstance(1);
        ibanOutcomeUnknownDTO.setStatus(IbanConstants.STATUS_UNKNOWN_PSP);

        RewardIban rewardIbanUnknown = RewardIban.builder()
                .id(IbanOutcomeDTO2RewardIbanMapper.buildId(ibanOutcomeUnknownDTO))
                .userId(ibanOutcomeUnknownDTO.getUserId())
                .initiativeId(ibanOutcomeUnknownDTO.getInitiativeId())
                .iban(ibanOutcomeUnknownDTO.getIban())
                .checkIbanOutcome(ibanOutcomeUnknownDTO.getStatus())
                .timestamp(LocalDateTime.now()).build();
        Mockito.when(ibanOutcomeDTO2RewardIbanMapperMock.apply(ibanOutcomeUnknownDTO)).thenReturn(rewardIbanUnknown);

        IbanOutcomeDTO ibanOutcomeOkDTO = IbanOutcomeDTOFaker.mockInstance(1);
        ibanOutcomeOkDTO.setStatus(IbanConstants.STATUS_OK);
        RewardIban rewardIbanOk = RewardIban.builder()
                .id(IbanOutcomeDTO2RewardIbanMapper.buildId(ibanOutcomeOkDTO))
                .userId(ibanOutcomeOkDTO.getUserId())
                .initiativeId(ibanOutcomeOkDTO.getInitiativeId())
                .iban(ibanOutcomeOkDTO.getIban())
                .checkIbanOutcome(ibanOutcomeOkDTO.getStatus())
                .timestamp(LocalDateTime.now()).build();
        Mockito.when(ibanOutcomeDTO2RewardIbanMapperMock.apply(ibanOutcomeOkDTO)).thenReturn(rewardIbanOk);

        Mockito.when(rewardIbanServiceMock.save(Mockito.any(RewardIban.class))).thenAnswer(i -> Mono.just(i.getArguments()[0]));

        // When
        RewardIban resultUnknown = ibanOutcomeOperationsService.execute(ibanOutcomeUnknownDTO).block();
        RewardIban resultOk = ibanOutcomeOperationsService.execute(ibanOutcomeOkDTO).block();

        // Then
        Assertions.assertNotNull(resultUnknown);
        Assertions.assertEquals(rewardIbanUnknown, resultUnknown);

        Assertions.assertNotNull(resultOk);
        Assertions.assertEquals(rewardIbanOk, resultOk);

        Mockito.verify(rewardIbanServiceMock, Mockito.times(2)).save(Mockito.any());
        Mockito.verify(rewardIbanServiceMock, Mockito.never()).deleteIban(Mockito.any());
    }
}