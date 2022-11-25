package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.test.fakers.RewardOrganizationImportFaker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

class RewardOrganizationImportsRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private RewardOrganizationImportsRepository repository;

    private final List<RewardOrganizationImport> testData = new ArrayList<>();

    @BeforeEach
    void storeTestData(){
        testData.add(repository.save(RewardOrganizationImportFaker.mockInstance(0)).block());
    }

    @AfterEach
    void clearTestData(){
        repository.deleteAll(testData);
    }

    @Test
    void testCreateIfNotExists(){
        // Given
        RewardOrganizationImport entity = RewardOrganizationImportFaker.mockInstance(0);

        // When already exists
        RewardOrganizationImport beforeDeleteResult = repository.createIfNotExistsOrReturnEmpty(entity).block();

        // Then beforeDeleteResult
        Assertions.assertNull(beforeDeleteResult);

        // Given
        repository.delete(entity).block();

        // When not exists
        RewardOrganizationImport afterDeleteResult = repository.createIfNotExistsOrReturnEmpty(entity).block();

        // Then afterDeleteResult
        Assertions.assertEquals(entity, afterDeleteResult);
    }

}
