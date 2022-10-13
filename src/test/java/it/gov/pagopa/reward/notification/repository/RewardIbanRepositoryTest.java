package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.model.RewardIban;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

class RewardIbanRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private RewardIbanRepository rewardIbanRepository;

    @Test
    void deleteByIdAndByIban() {

        String userId = "userId_prova";
        String initiativeId = "initiativeId_prova";
        String id = userId.concat(initiativeId);
        String iban = "iban_prova";
        LocalDateTime timestamp = LocalDateTime.of(2022, 10, 13, 17, 3, 43);

        RewardIban rewardIban = RewardIban.builder()
                .id(id)
                .userId(userId)
                .initiativeId(initiativeId)
                .iban(iban)
                .timestamp(timestamp)
                .build();

        rewardIbanRepository.save(rewardIban).block();

        RewardIban rewardIntoDBBeforeDelete = rewardIbanRepository.findById(rewardIban.getId()).block();
        Assertions.assertNotNull(rewardIntoDBBeforeDelete);
        Assertions.assertEquals(rewardIntoDBBeforeDelete, rewardIban);


        RewardIban result = rewardIbanRepository.deleteByIdAndIban(id, iban).block();
        Assertions.assertNotNull(result);
        RewardIban rewardIntoDBAfterDelete = rewardIbanRepository.findById(id).block();
        Assertions.assertNull(rewardIntoDBAfterDelete);

    }
}