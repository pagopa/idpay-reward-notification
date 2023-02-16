package it.gov.pagopa.reward.notification.service.csv.in.retrieve;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationImportCsvDto;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationExportStatus;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationImportResult;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationExportsRepository;
import it.gov.pagopa.reward.notification.service.csv.in.utils.RewardNotificationFeedbackExportDelta;
import it.gov.pagopa.reward.notification.test.fakers.RewardOrganizationExportsFaker;
import it.gov.pagopa.reward.notification.test.fakers.RewardsNotificationFaker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ExtendWith(MockitoExtension.class)
class RewardNotificationExportFeedbackRetrieverServiceTest {

    @Mock private RewardOrganizationExportsRepository repositoryMock;

    private RewardNotificationExportFeedbackRetrieverService service;

    @BeforeEach
    void init(){
        service = new RewardNotificationExportFeedbackRetrieverServiceImpl(repositoryMock);
    }

    @AfterEach
    void checkMocks(){
        Mockito.verifyNoMoreInteractions(repositoryMock);
    }

    //region test retrieve
    @Test
    void testRetrieve_NotFound(){
        // Given
        RewardOrganizationImport importRequest = new RewardOrganizationImport();
        importRequest.setInitiativeId("INITITATIVEID");
        importRequest.setOrganizationId("ORGANIZATIONID");

        RewardNotificationImportCsvDto row = new RewardNotificationImportCsvDto();
        row.setUniqueID("EXTERNALID");

        RewardsNotification rn = RewardsNotificationFaker.mockInstance(0);
        rn.setExportId("EXPORTID");
        rn.setInitiativeId("INITITATIVEID");
        rn.setOrganizationId("ORGANIZATIONID");
        rn.setExportDate(LocalDateTime.now());

        Mockito.when(repositoryMock.findById(rn.getExportId())).thenReturn(Mono.empty());

        Mockito.when(repositoryMock.save(Mockito.any())).thenAnswer(a->Mono.just(a.getArgument(0)));

        Map<String, RewardOrganizationExport> exportCache = new ConcurrentHashMap<>();

        // When
        RewardOrganizationExport result = service.retrieve(rn, row, importRequest, exportCache)
                .flatMap(x -> service.retrieve(rn, row, importRequest, exportCache)) // called again to test cache behavior
                .block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(
                RewardOrganizationExport.builder()
                        .id(rn.getExportId())
                        .initiativeId(importRequest.getInitiativeId())
                        .organizationId(importRequest.getOrganizationId())
                        .notificationDate(rn.getNotificationDate())
                        .exportDate(rn.getExportDate().toLocalDate())
                        .rewardsExportedCents(-1L)
                        .rewardNotified(-1L)
                        .status(RewardOrganizationExportStatus.EXPORTED)
                        .build(),
                result
        );

        Assertions.assertEquals(1, exportCache.size());
        Assertions.assertSame(result, exportCache.get(rn.getExportId()));

        Mockito.verify(repositoryMock).findById(Mockito.anyString());
    }

    @Test
    void testRetrieve_Success(){
        // Given
        RewardOrganizationImport importRequest = new RewardOrganizationImport();
        importRequest.setInitiativeId("INITITATIVEID");
        importRequest.setOrganizationId("ORGANIZATIONID");

        RewardNotificationImportCsvDto row = new RewardNotificationImportCsvDto();
        row.setUniqueID("EXTERNALID");

        RewardsNotification rn = RewardsNotificationFaker.mockInstance(0);
        rn.setExportId("EXPORTID");
        rn.setInitiativeId("INITITATIVEID");
        rn.setOrganizationId("ORGANIZATIONID");
        rn.setExportDate(LocalDateTime.now());

        RewardOrganizationExport expectedExport = RewardOrganizationExportsFaker.mockInstance(0);

        Mockito.when(repositoryMock.findById(rn.getExportId())).thenReturn(Mono.just(expectedExport));

        Map<String, RewardOrganizationExport> exportCache = new ConcurrentHashMap<>();

        // When
        RewardOrganizationExport result = service.retrieve(rn, row, importRequest, exportCache)
                .flatMap(x -> service.retrieve(rn, row, importRequest, exportCache)) // called again to test cache behavior
                .block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertSame(expectedExport, result);

        Assertions.assertEquals(1, exportCache.size());
        Assertions.assertSame(result, exportCache.get(rn.getExportId()));

        Mockito.verify(repositoryMock).findById(Mockito.anyString());
    }
    //endregion

    //region test updateCounters
    @Test
    void testUpdateCounters_firstFeedbackOK(){
        // Given
        RewardOrganizationExport export = new RewardOrganizationExport();

        RewardsNotification rn = RewardsNotificationFaker.mockInstance(0);
        rn.setStatus(RewardNotificationStatus.COMPLETED_OK);
        rn.setFeedbackHistory(List.of(new RewardsNotification.RewardNotificationHistory()));
        rn.setRewardCents(10_00L);

        long expectedDeltaRewardCents = rn.getRewardCents();

        Mockito.when(repositoryMock.updateCounters(Mockito.eq(1L), Mockito.eq(expectedDeltaRewardCents), Mockito.eq(1L), Mockito.same(export))).thenReturn(Mono.just(Mockito.mock(UpdateResult.class)));

        // When
        RewardNotificationFeedbackExportDelta result = service.updateCounters(rn, export).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(new RewardNotificationFeedbackExportDelta(export.getId(), 1L, 1L, expectedDeltaRewardCents), result);
    }

    @Test
    void testUpdateCounters_firstFeedbackKO(){
        // Given
        RewardOrganizationExport export = new RewardOrganizationExport();

        RewardsNotification rn = RewardsNotificationFaker.mockInstance(0);
        rn.setStatus(RewardNotificationStatus.COMPLETED_KO);
        rn.setFeedbackHistory(List.of(new RewardsNotification.RewardNotificationHistory()));
        rn.setRewardCents(10_00L);

        long expectedDeltaRewardCents = 0L;

        Mockito.when(repositoryMock.updateCounters(Mockito.eq(1L), Mockito.eq(expectedDeltaRewardCents), Mockito.eq(0L), Mockito.same(export))).thenReturn(Mono.just(Mockito.mock(UpdateResult.class)));

        // When
        RewardNotificationFeedbackExportDelta result = service.updateCounters(rn, export).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(new RewardNotificationFeedbackExportDelta(export.getId(), 1L, 0L, expectedDeltaRewardCents), result);
    }

    @Test
    void testUpdateCounters_Ko2Ko(){
        // Given
        RewardOrganizationExport export = new RewardOrganizationExport();

        RewardsNotification rn = RewardsNotificationFaker.mockInstance(0);
        rn.setStatus(RewardNotificationStatus.COMPLETED_KO);
        rn.setFeedbackHistory(List.of(
                RewardsNotification.RewardNotificationHistory.builder()
                        .result(RewardOrganizationImportResult.KO)
                        .build(),
                new RewardsNotification.RewardNotificationHistory()));
        rn.setRewardCents(10_00L);

        long expectedDeltaRewardCents = 0L;

        Mockito.when(repositoryMock.updateCounters(Mockito.eq(0L), Mockito.eq(expectedDeltaRewardCents), Mockito.eq(0L), Mockito.same(export))).thenReturn(Mono.just(Mockito.mock(UpdateResult.class)));

        // When
        RewardNotificationFeedbackExportDelta result = service.updateCounters(rn, export).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(new RewardNotificationFeedbackExportDelta(export.getId(), 0L, 0L, expectedDeltaRewardCents), result);
    }

    @Test
    void testUpdateCounters_Ok2Ok(){
        // Given
        RewardOrganizationExport export = new RewardOrganizationExport();

        RewardsNotification rn = RewardsNotificationFaker.mockInstance(0);
        rn.setStatus(RewardNotificationStatus.COMPLETED_OK);
        rn.setFeedbackHistory(List.of(
                RewardsNotification.RewardNotificationHistory.builder()
                        .result(RewardOrganizationImportResult.KO)
                        .build(),
                RewardsNotification.RewardNotificationHistory.builder()
                        .result(RewardOrganizationImportResult.OK)
                        .build(),
                new RewardsNotification.RewardNotificationHistory()));
        rn.setRewardCents(10_00L);

        long expectedDeltaRewardCents = 0L;

        Mockito.when(repositoryMock.updateCounters(Mockito.eq(0L), Mockito.eq(expectedDeltaRewardCents), Mockito.eq(0L), Mockito.same(export))).thenReturn(Mono.just(Mockito.mock(UpdateResult.class)));

        // When
        RewardNotificationFeedbackExportDelta result = service.updateCounters(rn, export).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(new RewardNotificationFeedbackExportDelta(export.getId(), 0L, 0L, expectedDeltaRewardCents), result);
    }

    @Test
    void testUpdateCounters_Ko2Ok(){
        // Given
        RewardOrganizationExport export = new RewardOrganizationExport();

        RewardsNotification rn = RewardsNotificationFaker.mockInstance(0);
        rn.setStatus(RewardNotificationStatus.COMPLETED_OK);
        rn.setFeedbackHistory(List.of(
                RewardsNotification.RewardNotificationHistory.builder()
                        .result(RewardOrganizationImportResult.KO)
                        .build(),
                RewardsNotification.RewardNotificationHistory.builder()
                        .result(RewardOrganizationImportResult.KO)
                        .build(),
                new RewardsNotification.RewardNotificationHistory()));
        rn.setRewardCents(10_00L);

        long expectedDeltaRewardCents = rn.getRewardCents();

        Mockito.when(repositoryMock.updateCounters(Mockito.eq(0L), Mockito.eq(expectedDeltaRewardCents), Mockito.eq(1L), Mockito.same(export))).thenReturn(Mono.just(Mockito.mock(UpdateResult.class)));

        // When
        RewardNotificationFeedbackExportDelta result = service.updateCounters(rn, export).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(new RewardNotificationFeedbackExportDelta(export.getId(), 0L, 1L, expectedDeltaRewardCents), result);
    }

    @Test
    void testUpdateCounters_Ok2Ko(){
        // Given
        RewardOrganizationExport export = new RewardOrganizationExport();

        RewardsNotification rn = RewardsNotificationFaker.mockInstance(0);
        rn.setStatus(RewardNotificationStatus.COMPLETED_KO);
        rn.setFeedbackHistory(List.of(
                RewardsNotification.RewardNotificationHistory.builder()
                        .result(RewardOrganizationImportResult.KO)
                        .build(),
                RewardsNotification.RewardNotificationHistory.builder()
                        .result(RewardOrganizationImportResult.OK)
                        .build(),
                new RewardsNotification.RewardNotificationHistory()));
        rn.setRewardCents(10_00L);

        long expectedDeltaRewardCents = -rn.getRewardCents();

        Mockito.when(repositoryMock.updateCounters(Mockito.eq(0L), Mockito.eq(expectedDeltaRewardCents), Mockito.eq(-1L), Mockito.same(export))).thenReturn(Mono.just(Mockito.mock(UpdateResult.class)));

        // When
        RewardNotificationFeedbackExportDelta result = service.updateCounters(rn, export).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(new RewardNotificationFeedbackExportDelta(export.getId(), 0L, -1L, expectedDeltaRewardCents), result);
    }
    //endregion

    @Test
    void testUpdateExportStatus(){
        // Given
        List<String> exportIds=List.of("EXPORTID1", "EXPORTID2", "EXPORTID3", "EXPORTID4");

        //region useCase1 EXPORTED -> COMPLETED, no percentage to fix
        RewardOrganizationExport export1 = RewardOrganizationExport.builder()
                .id("useCase1")

                .rewardNotified(10L)
                .rewardsExportedCents(10_00L)

                .rewardsResulted(10L)
                .rewardsResultedOk(10L)
                .rewardsResultsCents(10_00L)

                .percentageResulted(100_00L)
                .percentageResultedOk(100_00L)
                .percentageResults(100_00L)

                .status(RewardOrganizationExportStatus.EXPORTED)
                .build();
        UpdateResult expectedUpdateResult1 = Mockito.mock(UpdateResult.class);

        Mockito.when(repositoryMock.updateStatus(Mockito.eq(RewardOrganizationExportStatus.COMPLETE), Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.same(export1)))
                .thenReturn(Mono.just(expectedUpdateResult1));
        //endregion

        //region useCase2 EXPORTED -> PARTIAL, percentage OK to fix
        RewardOrganizationExport export2 = RewardOrganizationExport.builder()
                .id("useCase2")

                .rewardNotified(10L)
                .rewardsExportedCents(10_00L)

                .rewardsResulted(9L)
                .rewardsResultedOk(9L)
                .rewardsResultsCents(9_00L)

                .percentageResulted(90_00L)
                .percentageResultedOk(90_01L)
                .percentageResults(90_00L)

                .status(RewardOrganizationExportStatus.EXPORTED)
                .build();
        UpdateResult expectedUpdateResult2 = Mockito.mock(UpdateResult.class);

        Mockito.when(repositoryMock.updateStatus(Mockito.eq(RewardOrganizationExportStatus.PARTIAL), Mockito.isNull(), Mockito.eq(90_00L), Mockito.isNull(), Mockito.same(export2)))
                .thenReturn(Mono.just(expectedUpdateResult2));
        //endregion

        //region useCase3 PARTIAL -> COMPLETED, all percentage but OK to fix
        RewardOrganizationExport export3 = RewardOrganizationExport.builder()
                .id("useCase3")

                .rewardNotified(10L)
                .rewardsExportedCents(10_00L)

                .rewardsResulted(10L)
                .rewardsResultedOk(10L)
                .rewardsResultsCents(10_00L)

                .percentageResulted(99_98L)
                .percentageResultedOk(100_00L)
                .percentageResults(99_98L)

                .status(RewardOrganizationExportStatus.PARTIAL)
                .build();
        UpdateResult expectedUpdateResult3 = Mockito.mock(UpdateResult.class);

        Mockito.when(repositoryMock.updateStatus(Mockito.eq(RewardOrganizationExportStatus.COMPLETE), Mockito.eq(100_00L), Mockito.isNull(), Mockito.eq(100_00L), Mockito.same(export3)))
                .thenReturn(Mono.just(expectedUpdateResult3));
        //endregion


        //region useCase4 PARTIAL -> PARTIAL
        RewardOrganizationExport export4 = RewardOrganizationExport.builder()
                .id("useCase4")

                .rewardNotified(10L)
                .rewardsExportedCents(10_00L)

                .rewardsResulted(9L)
                .rewardsResultedOk(9L)
                .rewardsResultsCents(9_00L)

                .percentageResulted(90_00L)
                .percentageResultedOk(90_00L)
                .percentageResults(90_00L)

                .status(RewardOrganizationExportStatus.PARTIAL)
                .build();
        //endregion

        Mockito.when(repositoryMock.findAllById(exportIds)).thenReturn(Flux.just(export1, export2, export3, export4));

        // When
        List<UpdateResult> results = service.updateExportStatus(exportIds).collectList().block();

        // Then
        Assertions.assertNotNull(results);

        Assertions.assertEquals(
                List.of(
                    expectedUpdateResult1,
                    expectedUpdateResult2,
                    expectedUpdateResult3,
                    UpdateResult.acknowledged(0, null, null)
                ),
                results
        );
    }
}
