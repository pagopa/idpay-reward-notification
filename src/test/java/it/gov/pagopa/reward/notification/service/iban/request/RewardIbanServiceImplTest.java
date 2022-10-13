package it.gov.pagopa.reward.notification.service.iban.request;

import it.gov.pagopa.reward.notification.model.RewardIban;
import it.gov.pagopa.reward.notification.repository.RewardIbanRepository;
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
}