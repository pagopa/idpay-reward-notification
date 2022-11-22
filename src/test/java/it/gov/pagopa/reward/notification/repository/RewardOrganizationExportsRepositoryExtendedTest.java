package it.gov.pagopa.reward.notification.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationExportStatus;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.service.csv.out.retrieve.Initiative2ExportRetrieverServiceImpl;
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
                        .exportDate(LocalDate.now().minusDays(1))
                        .status(RewardOrganizationExportStatus.IN_PROGRESS)
                        .build(),
                RewardOrganizationExport.builder()
                        .id("ID2")
                        .initiativeId("INITIATIVEID2")
                        .organizationId("ORGANIZATIONID")
                        .notificationDate(LocalDate.now())
                        .status(RewardOrganizationExportStatus.TO_DO)
                        .build(),
                RewardOrganizationExport.builder()
                        .id("ID3")
                        .initiativeId("INITIATIVEID3")
                        .organizationId("ORGANIZATIONID")
                        .notificationDate(LocalDate.now())
                        .status(RewardOrganizationExportStatus.EXPORTED)

                        .rewardNotified(100L)
                        .rewardsExportedCents(100_00L)

                        .rewardsResulted(10L)
                        .rewardsResultedOk(5L)
                        .rewardsResultsCents(3_00L)

                        .percentageResulted(10_00L)  // 10%
                        .percentageResultedOk(5_00L) // 5%
                        .percentageResults(3_00L)    // 3%

                        .build()
        ));

        repository.saveAll(testData).collectList().block();
    }

    @AfterEach
    void clearData(){
        repository.deleteAllById(testData.stream().map(RewardOrganizationExport::getId).toList()).block();
    }

    @Test
    void reserveStuckExportTest(){
        List<RewardOrganizationExport> stuckBefore = repository.findAll(Example.of(RewardOrganizationExport.builder().status(RewardOrganizationExportStatus.IN_PROGRESS).build())).collectList().block();
        Assertions.assertNotNull(stuckBefore);
        Assertions.assertEquals(1, stuckBefore.size());
        Assertions.assertEquals(testData.get(0), stuckBefore.get(0));

        RewardOrganizationExport result = repository.reserveStuckExport().block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("ID1", result.getId());
        Assertions.assertEquals(RewardOrganizationExportStatus.IN_PROGRESS, result.getStatus());
        Assertions.assertEquals(LocalDate.now(), result.getExportDate());

        List<RewardOrganizationExport> inProgressAfter = repository.findAll(Example.of(RewardOrganizationExport.builder().status(RewardOrganizationExportStatus.IN_PROGRESS).build())).collectList().block();
        Assertions.assertNotNull(inProgressAfter);
        Assertions.assertEquals("ID1", result.getId());
        Assertions.assertEquals(RewardOrganizationExportStatus.IN_PROGRESS, result.getStatus());
        Assertions.assertEquals(LocalDate.now(), result.getExportDate());

        RewardOrganizationExport resultWhenNoStuck = repository.reserveStuckExport().block();
        Assertions.assertNull(resultWhenNoStuck);
    }

    @Test
    void reserveExportTest(){
        List<RewardOrganizationExport> todoBefore = repository.findAll(Example.of(RewardOrganizationExport.builder().status(RewardOrganizationExportStatus.TO_DO).build())).collectList().block();
        Assertions.assertNotNull(todoBefore);
        Assertions.assertEquals(1, todoBefore.size());
        Assertions.assertEquals("ID2", todoBefore.get(0).getId());

        RewardOrganizationExport result = repository.reserveExport().block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("ID2", result.getId());
        Assertions.assertEquals(RewardOrganizationExportStatus.IN_PROGRESS, result.getStatus());
        Assertions.assertEquals(LocalDate.now(), result.getExportDate());

        List<RewardOrganizationExport> todoAfter = repository.findAll(Example.of(RewardOrganizationExport.builder().status(RewardOrganizationExportStatus.TO_DO).build())).collectList().block();
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

//region test updateCounters
    @Test
    void testUpdateCounters_noExists(){
        // Given
        RewardOrganizationExport export = new RewardOrganizationExport();
        export.setId("NEVERSEENID");
        export.setRewardNotified(10L);
        export.setRewardsExportedCents(10_00L);

        // When
        UpdateResult result = repository.updateCounters(1L, 1L, 1L, export).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(UpdateResult.acknowledged(0, 0L, null), result);
    }

    @Test
    void testUpdateCounters_noChanges(){
        // Given
        long incReward = 0L;
        int inc = 0;
        int incOk = 0;
        RewardOrganizationExport export = testData.get(2);

        // When
        UpdateResult result = repository.updateCounters(inc, incReward, incOk, export).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(UpdateResult.acknowledged(0, null, null), result);

        Assertions.assertEquals(export, repository.findById(export.getId()).block());
    }

    @Test
    void testUpdateCounters_incRewardCents(){
        testUpdateCounters(10_00L, 0, 0, 13_00, 10_00L, 5_00L);
    }

    @Test
    void testUpdateCounters_incCount(){
        testUpdateCounters(0L, 10, 0, 3_00, 20_00L, 5_00L);
    }

    @Test
    void testUpdateCounters_incCountOk(){
        testUpdateCounters(0L, 0, 10, 3_00, 10_00L, 15_00L);
    }

    @Test
    void testUpdateCounters_incAll(){
        testUpdateCounters(-1_00, -1, -1, 2_00, 9_00L, 4_00L);
    }

    void testUpdateCounters(long rewardCents, int inc, int incOk, long expectedPercentageRewardsCents, long expectedPercentageResulted, long expectedPercentageResultedOk){
        // Given
        RewardOrganizationExport export = testData.get(2);

        // When
        UpdateResult result = repository.updateCounters(inc, rewardCents, incOk, export).block();

        // Then
        checkUpdateCounters(rewardCents, inc, incOk, expectedPercentageRewardsCents, expectedPercentageResulted, expectedPercentageResultedOk, export, result);
    }

    private void checkUpdateCounters(long rewardCents, int inc, int incOk, long expectedPercentageRewardsCents, long expectedPercentageResulted, long expectedPercentageResultedOk, RewardOrganizationExport export, UpdateResult result) {
        Assertions.assertNotNull(result);
        Assertions.assertEquals(UpdateResult.acknowledged(1, 1L, null), result);

        Assertions.assertEquals(
                export.toBuilder()
                        .rewardsResulted(export.getRewardsResulted() + inc)
                        .rewardsResultedOk(export.getRewardsResultedOk() + incOk)
                        .rewardsResultsCents(export.getRewardsResultsCents() + rewardCents)

                        .percentageResulted(expectedPercentageResulted)
                        .percentageResultedOk(expectedPercentageResultedOk)
                        .percentageResults(expectedPercentageRewardsCents)

                        .build(),
                repository.findById(export.getId()).block());
    }
//endregion

//region test updateCountersOnRewardFeedback
    @Test
    void testUpdateCountersOnRewardFeedback_NoExists(){
        // Given
        RewardOrganizationExport export = new RewardOrganizationExport();
        export.setId("NEVERSEENID");
        export.setRewardNotified(10L);
        export.setRewardsExportedCents(10_00L);

        // When
        UpdateResult result = repository.updateCountersOnRewardFeedback(true, 10L, export).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(UpdateResult.acknowledged(0, 0L, null), result);
    }

    @Test
    void testUpdateCountersOnRewardFeedback_firstFeedbackOk(){
        // Given
        boolean firstFeedback = true;
        long deltaRewardCents = 9_37L;
        RewardOrganizationExport export = testData.get(2);

        // When
        UpdateResult result = repository.updateCountersOnRewardFeedback(firstFeedback, deltaRewardCents, export).block();

        // Then
        checkUpdateCounters(deltaRewardCents, 1, 1
                , 12_37L, 11_00L, 6_00L
                , export, result);
    }

    @Test
    void testUpdateCountersOnRewardFeedback_firstFeedbackKO(){
        // Given
        boolean firstFeedback = true;
        long deltaRewardCents = 0;
        RewardOrganizationExport export = testData.get(2);

        // When
        UpdateResult result = repository.updateCountersOnRewardFeedback(firstFeedback, deltaRewardCents, export).block();

        // Then
        checkUpdateCounters(deltaRewardCents, 1, 0
                , 3_00L, 11_00L, 5_00L
                , export, result);
    }

    @Test
    void testUpdateCountersOnRewardFeedback_noStatusChange(){
        // Given
        boolean firstFeedback = false;
        long deltaRewardCents = 0L;
        RewardOrganizationExport export = testData.get(2);

        // When
        UpdateResult result = repository.updateCountersOnRewardFeedback(firstFeedback, deltaRewardCents, export).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(UpdateResult.acknowledged(0, null, null), result);
    }

    @Test
    void testUpdateCountersOnRewardFeedback_Ok2Ko(){
        // Given
        boolean firstFeedback = false;
        long deltaRewardCents = -1_37L;
        RewardOrganizationExport export = testData.get(2);

        // When
        UpdateResult result = repository.updateCountersOnRewardFeedback(firstFeedback, deltaRewardCents, export).block();

        // Then
        checkUpdateCounters(deltaRewardCents, 0, -1
                , 1_63L, 10_00L, 4_00L
                , export, result);
    }

    @Test
    void testUpdateCountersOnRewardFeedback_Ko2Ok(){
        // Given
        boolean firstFeedback = false;
        long deltaRewardCents = 1_37L;
        RewardOrganizationExport export = testData.get(2);

        // When
        UpdateResult result = repository.updateCountersOnRewardFeedback(firstFeedback, deltaRewardCents, export).block();

        // Then
        checkUpdateCounters(deltaRewardCents, 0, 1
                , 4_37L, 10_00L, 6_00L
                , export, result);
    }
//endregion
}
