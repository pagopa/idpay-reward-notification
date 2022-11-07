package it.gov.pagopa.reward.notification.service.csv.export;

import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationExportCsvDto;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.csv.export.mapper.RewardNotification2ExportCsvService;
import it.gov.pagopa.reward.notification.service.csv.export.writer.ExportCsvFinalizeService;
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

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ExtendWith(MockitoExtension.class)
class ExportInitiativeRewardsServiceTest {

    private final int csvMaxRows = 100;

    @Mock
    private RewardsNotificationRepository rewardsNotificationRepositoryMock;
    @Mock
    private RewardNotification2ExportCsvService reward2CsvLineServiceMock;
    @Mock
    private ExportCsvFinalizeService csvWriterServiceMock;

    private ExportInitiativeRewardsService service;

    @BeforeEach
    void init() {
        service = new ExportInitiativeRewardsServiceImpl(csvMaxRows, rewardsNotificationRepositoryMock, reward2CsvLineServiceMock, csvWriterServiceMock);
    }

    @AfterEach
    void verifyNoMoreMockInvocations() {
        Mockito.verifyNoMoreInteractions(rewardsNotificationRepositoryMock, reward2CsvLineServiceMock, csvWriterServiceMock);
    }

    @Test
    void testStuckExport() {
        // Given
        int stuckRewards = csvMaxRows - 10;
        int newRewards = csvMaxRows + 5;

        RewardOrganizationExport stuckExport = new RewardOrganizationExport();
        stuckExport.setId("EXPORTID");
        stuckExport.setInitiativeId("INITIATIVEID");
        stuckExport.setNotificationDate(LocalDate.now().minusDays(7));
        stuckExport.setExportDate(LocalDate.now());

        List<RewardsNotification> stuckRewardNotification = IntStream.range(0, stuckRewards)
                .mapToObj(i -> RewardsNotificationFaker.mockInstanceBuilder(i)
                        .id("STUCKREWARDS%d".formatted(i))
                        .exportId(stuckExport.getId())
                        .status(RewardNotificationStatus.EXPORTED)
                        .build())
                .toList();
        Mockito.when(rewardsNotificationRepositoryMock.findExportRewards(stuckExport.getId())).thenReturn(Flux.fromIterable(stuckRewardNotification));

        List<RewardsNotification> rewardNotification2Notify = IntStream.range(stuckRewards, stuckRewards + newRewards).mapToObj(RewardsNotificationFaker::mockInstance).toList();
        Mockito.when(rewardsNotificationRepositoryMock.findRewards2Notify(stuckExport.getInitiativeId(), stuckExport.getNotificationDate())).thenReturn(Flux.fromIterable(rewardNotification2Notify));

        Mockito.when(reward2CsvLineServiceMock.apply(Mockito.any())).thenAnswer(i ->
                Mono.just(RewardNotificationExportCsvDto.builder().uniqueID(i.getArgument(0, RewardsNotification.class).getId()).build()));

        Mockito.when(csvWriterServiceMock.writeCsvAndFinalize(Mockito.any(), Mockito.same(stuckExport))).thenAnswer(i -> Mono.just(i.getArgument(1, RewardOrganizationExport.class)));

        // When
        List<RewardOrganizationExport> result = service.performExport(stuckExport, true).collectList().block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size()); // expected 2 split

        stuckRewardNotification.forEach(r -> Mockito.verify(reward2CsvLineServiceMock).apply(Mockito.same(r)));

        // new Ids to be verified
        Set<String> newRewardIdsExported = new HashSet<>();

        // the first split (the only having maxSize) should contain the entire stuck rewards and some new rewards
        Mockito.verify(csvWriterServiceMock).writeCsvAndFinalize(Mockito.argThat(csvLines -> {
            // the first split should have the max size
            if (csvMaxRows != csvLines.size()) {
                return false;
            }

            // The first lines should be (in any order) the stuck rewards
            Assertions.assertEquals(
                    stuckRewardNotification.stream().map(RewardsNotification::getId).collect(Collectors.toSet()),
                    csvLines.subList(0, stuckRewards).stream().map(RewardNotificationExportCsvDto::getUniqueID).collect(Collectors.toSet())
            );

            // the remaining rows are new records
            newRewardIdsExported.addAll(csvLines.subList(stuckRewards, csvLines.size()).stream().map(RewardNotificationExportCsvDto::getUniqueID).toList());
            Assertions.assertEquals(csvMaxRows - stuckRewards, newRewardIdsExported.size());

            return true;
        }), Mockito.same(stuckExport));

        // the second split should not have the max size
        Mockito.verify(csvWriterServiceMock).writeCsvAndFinalize(Mockito.argThat(csvLines -> {
            if (csvMaxRows == csvLines.size()) {
                return false;
            }
            Assertions.assertEquals(
                    // just the last export has not the max size
                    (stuckRewards + newRewards) % csvMaxRows,
                    csvLines.size());

            newRewardIdsExported.addAll(csvLines.stream().map(RewardNotificationExportCsvDto::getUniqueID).toList());

            return true;
        }), Mockito.same(stuckExport));// TODO it will change after splits implementation

        Assertions.assertEquals(
                rewardNotification2Notify.stream().map(RewardsNotification::getId).collect(Collectors.toSet()),
                newRewardIdsExported);
    }



    @Test
    void testNotStuckExport() {
        // Given
        int newRewards = csvMaxRows + 10;

        RewardOrganizationExport export = new RewardOrganizationExport();
        export.setId("EXPORTID");
        export.setInitiativeId("INITIATIVEID");
        export.setNotificationDate(LocalDate.now());
        export.setExportDate(LocalDate.now());

        Mockito.when(rewardsNotificationRepositoryMock.findExportRewards(export.getId())).thenReturn(Flux.empty());

        List<RewardsNotification> rewardNotification2Notify = IntStream.range(0, newRewards).mapToObj(RewardsNotificationFaker::mockInstance).toList();
        Mockito.when(rewardsNotificationRepositoryMock.findRewards2Notify(export.getInitiativeId(), export.getNotificationDate())).thenReturn(Flux.fromIterable(rewardNotification2Notify));

        Mockito.when(reward2CsvLineServiceMock.apply(Mockito.any())).thenAnswer(i ->
                Mono.just(RewardNotificationExportCsvDto.builder().uniqueID(i.getArgument(0, RewardsNotification.class).getId()).build()));

        Mockito.when(csvWriterServiceMock.writeCsvAndFinalize(Mockito.any(), Mockito.same(export))).thenAnswer(i -> Mono.just(i.getArgument(1, RewardOrganizationExport.class)));

        // When
        List<RewardOrganizationExport> result = service.performExport(export, true).collectList().block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size()); // expected 2 split

        // new Ids to be verified
        Set<String> newRewardIdsExported = new HashSet<>();

        // the first split is the only having maxSize
        Mockito.verify(csvWriterServiceMock).writeCsvAndFinalize(Mockito.argThat(csvLines -> {
            // the first split should have the max size
            if (csvMaxRows != csvLines.size()) {
                return false;
            }

            // the remaining rows are new records
            newRewardIdsExported.addAll(csvLines.stream().map(RewardNotificationExportCsvDto::getUniqueID).toList());
            Assertions.assertEquals(csvMaxRows, newRewardIdsExported.size());

            return true;
        }), Mockito.same(export));

        // the second split should not have the max size
        Mockito.verify(csvWriterServiceMock).writeCsvAndFinalize(Mockito.argThat(csvLines -> {
            if (csvMaxRows == csvLines.size()) {
                return false;
            }
            Assertions.assertEquals(
                    // just the last export has not the max size
                    (newRewards) % csvMaxRows,
                    csvLines.size());

            newRewardIdsExported.addAll(csvLines.stream().map(RewardNotificationExportCsvDto::getUniqueID).toList());

            return true;
        }), Mockito.same(export));// TODO it will change after splits implementation

        Assertions.assertEquals(
                rewardNotification2Notify.stream().map(RewardsNotification::getId).collect(Collectors.toSet()),
                newRewardIdsExported);
    }
}
