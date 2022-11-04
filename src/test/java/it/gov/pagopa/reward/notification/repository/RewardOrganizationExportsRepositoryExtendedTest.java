package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.enums.ExportStatus;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.service.csv.export.retrieve.Initiative2ExportRetrieverServiceImpl;
import it.gov.pagopa.reward.notification.test.fakers.RewardNotificationRuleFaker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class RewardOrganizationExportsRepositoryExtendedTest extends BaseIntegrationTest {

    @Autowired
    private RewardOrganizationExportsRepository repository;
    @Autowired
    private Initiative2ExportRetrieverServiceImpl initiative2ExportRetrieverService;

    private List<RewardOrganizationExport> testData;

    @BeforeEach
    void createTestData(){
        testData = new ArrayList<>(List.of(
                RewardOrganizationExport.builder()
                        .id("ID1")
                        .initiativeId("INITIATIVEID")
                        .organizationId("ORGANIZATIONID")
                        .notificationDate(LocalDate.now().minusDays(1))
                        .status(ExportStatus.IN_PROGRESS)
                        .build(),
                RewardOrganizationExport.builder()
                        .id("ID2")
                        .initiativeId("INITIATIVEID2")
                        .organizationId("ORGANIZATIONID")
                        .notificationDate(LocalDate.now())
                        .status(ExportStatus.TODO)
                        .build(),
                RewardOrganizationExport.builder()
                        .id("ID3")
                        .initiativeId("INITIATIVEID3")
                        .organizationId("ORGANIZATIONID")
                        .notificationDate(LocalDate.now())
                        .status(ExportStatus.EXPORTED)
                        .build()
        ));

        repository.saveAll(testData).collectList().block();
    }

    @AfterEach
    void clearData(){
        repository.deleteAllById(testData.stream().map(RewardOrganizationExport::getId).toList()).block();
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

    @Test
    void findPendingAndTodayExportsTest(){
        Assertions.assertEquals(
                List.of("ID1","ID2","ID3"),
                repository.findPendingAndTodayExports().map(RewardOrganizationExport::getId).sort().collectList().block()
        );
    }

//region configureNewExport test
    @Test
    void configureNewExport(){
        RewardNotificationRule rule = RewardNotificationRuleFaker.mockInstance(0);
        rule.setInitiativeId("INITIATIVE_NOT_EXPORTED_YET");

        RewardOrganizationExport newExport = initiative2ExportRetrieverService.buildNewRewardOrganizationExportEntity(rule, LocalDate.now(), 0L);
        testData.add(newExport);

        RewardOrganizationExport result = repository.configureNewExport(newExport).block();
        Assertions.assertSame(newExport, result);

        RewardOrganizationExport stored = repository.findById(newExport.getId()).block();
        Assertions.assertEquals(newExport, stored);
    }

    @Test
    void configureNewExport_whenExistPending(){
        RewardNotificationRule rule = RewardNotificationRuleFaker.mockInstance(0);
        rule.setInitiativeId("INITIATIVEID2");

        RewardOrganizationExport newExport = initiative2ExportRetrieverService.buildNewRewardOrganizationExportEntity(rule, LocalDate.now(), 0L);
        testData.add(newExport);

        RewardOrganizationExport result = repository.configureNewExport(newExport).block();
        Assertions.assertNull(result);

        Assertions.assertNull(repository.findById(newExport.getId()).block());
        Assertions.assertEquals(1L, repository.count(Example.of(RewardOrganizationExport.builder().initiativeId("INITIATIVEID2").build())).block());
    }
//endregion
}
