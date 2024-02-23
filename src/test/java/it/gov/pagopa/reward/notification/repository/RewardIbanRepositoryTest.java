package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.BaseIntegrationTestDeprecated;
import it.gov.pagopa.reward.notification.model.RewardIban;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

class RewardIbanRepositoryTest extends BaseIntegrationTestDeprecated {

    private final String userId = "userId_prova";
    private final String initiativeId = "initiativeId_prova";
    private final String id = userId.concat(initiativeId);
    private final String iban = "iban_prova";
    private final LocalDateTime timestamp = LocalDateTime.of(2022, 10, 13, 17, 3, 43);

    @Autowired
    private RewardIbanRepository rewardIbanRepository;

    private RewardIban rewardIban;

    @BeforeEach
    void setUo(){
        rewardIban = RewardIban.builder()
                .id(id)
                .userId(userId)
                .initiativeId(initiativeId)
                .iban(iban)
                .timestamp(timestamp)
                .build();
    }

    @AfterEach
    void cleanData(){
        rewardIbanRepository.deleteByIdAndIban(userId.concat(initiativeId), iban);
    }

    @Test
    void deleteByIdAndByIban() {
        rewardIbanRepository.save(rewardIban).block();

        checkDBBeforeDelete();


        RewardIban result = rewardIbanRepository.deleteByIdAndIban(id, iban).block();
        Assertions.assertNotNull(result);
        RewardIban rewardIntoDBAfterDelete = rewardIbanRepository.findById(id).block();
        Assertions.assertNull(rewardIntoDBAfterDelete);
    }

    @Test
    void deleteByIdAndByIbanNotPresent() {
        rewardIbanRepository.save(rewardIban).block();

        checkDBBeforeDelete();

        RewardIban result = rewardIbanRepository.deleteByIdAndIban(id, "IBAN_2").block();
        Assertions.assertNull(result);
        RewardIban rewardIntoDBAfterDelete = rewardIbanRepository.findById(id).block();
        Assertions.assertNotNull(rewardIntoDBAfterDelete);
        Assertions.assertEquals(rewardIntoDBAfterDelete, rewardIban);
    }

    private void checkDBBeforeDelete() {
        RewardIban rewardIntoDBBeforeDelete = rewardIbanRepository.findById(rewardIban.getId()).block();
        Assertions.assertNotNull(rewardIntoDBBeforeDelete);
        Assertions.assertEquals(rewardIntoDBBeforeDelete, rewardIban);
    }
}