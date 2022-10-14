package it.gov.pagopa.reward.notification.service.iban;

import it.gov.pagopa.reward.notification.model.RewardIban;
import it.gov.pagopa.reward.notification.repository.RewardIbanRepository;
import it.gov.pagopa.reward.notification.service.utils.IbanConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

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

        RewardIban rewardIban1 = RewardIban.builder()
                .id("USERIDINITIATIVEID1")
                .userId("USERID1")
                .initiativeId("INITIATIVEID1")
                .iban("IBAN1")
                .checkIbanOutcome(IbanConstants.STATUS_KO)
                .build();

        RewardIban rewardIban2 = RewardIban.builder()
                .id("USERIDINITIATIVEID2")
                .userId("USERID2")
                .initiativeId("INITIATIVEID2")
                .iban("IBAN2")
                .checkIbanOutcome(IbanConstants.STATUS_KO)
                .build();

        Mockito.when(rewardIbanRepositoryMock.deleteByIdAndIban(Mockito.same(rewardIban1.getId()), Mockito.same(rewardIban1.getIban()))).thenReturn(Mono.just(rewardIban1));
        Mockito.when(rewardIbanRepositoryMock.deleteByIdAndIban(Mockito.same(rewardIban2.getId()), Mockito.same(rewardIban2.getIban()))).thenReturn(Mono.empty());

        // When
        RewardIban result1 = rewardIbanService.deleteIban(rewardIban1).block();
        RewardIban result2 = rewardIbanService.deleteIban(rewardIban2).block();

        // Then
        Assertions.assertNotNull(result1);
        Assertions.assertEquals(rewardIban1, result1);

        Assertions.assertNull(result2);

        Mockito.verify(rewardIbanRepositoryMock, Mockito.times(2)).deleteByIdAndIban(Mockito.anyString(),Mockito.anyString());
    }
}