package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.enums.ExportStatus;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;

import java.util.Collections;
import java.util.List;

class RewardOrganizationExportsRepositoryExtendedTest extends BaseIntegrationTest {

    @Autowired
    private RewardOrganizationExportsRepository repository;

    @BeforeEach
    void createTestData(){
        repository.saveAll(List.of(
                RewardOrganizationExport.builder()
                        .id("ID1")
                        .initiativeId("INITIATIVEID")
                        .organizationId("ORGANIZATIONID")
                        .status(ExportStatus.IN_PROGRESS)
                .build(),
                RewardOrganizationExport.builder()
                        .id("ID2")
                        .initiativeId("INITIATIVEID2")
                        .organizationId("ORGANIZATIONID")
                        .status(ExportStatus.TODO)
                        .build()
        )).collectList().block();
    }

    @AfterEach
    void clearData(){
        repository.deleteAllById(List.of("ID1","ID2")).block();
    }

    @Test
    void reserveExportTest(){
        List<RewardOrganizationExport> todoBefore = repository.findAll(Example.of(RewardOrganizationExport.builder().status(ExportStatus.TODO).build())).collectList().block();
        Assertions.assertNotNull(todoBefore);
        Assertions.assertEquals(1, todoBefore.size());
        Assertions.assertEquals("ID2", todoBefore.get(0).getId());

        RewardOrganizationExport result = repository.reserveExport().block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("ID2", result.getId());


        List<RewardOrganizationExport> todoAfter = repository.findAll(Example.of(RewardOrganizationExport.builder().status(ExportStatus.TODO).build())).collectList().block();
        Assertions.assertNotNull(todoAfter);
        Assertions.assertEquals(Collections.emptyList(), todoAfter);

        RewardOrganizationExport resultWhenNoMoreTodo = repository.reserveExport().block();
        Assertions.assertNull(resultWhenNoMoreTodo);
    }
}
