package it.gov.pagopa.reward.notification.service.csv.out;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationExportCsvDto;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationExportStatus;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationExportsRepository;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.csv.out.mapper.RewardNotification2ExportCsvService;
import it.gov.pagopa.reward.notification.service.csv.out.retrieve.Initiative2ExportRetrieverService;
import it.gov.pagopa.reward.notification.service.csv.out.writer.ExportCsvFinalizeService;
import it.gov.pagopa.reward.notification.service.suspension.UserSuspensionService;
import it.gov.pagopa.reward.notification.test.fakers.RewardsNotificationFaker;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import wiremock.org.eclipse.jetty.util.BlockingArrayQueue;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ExportInitiativeRewardsServiceTest {

    public static final String INITIATIVEID = "INITIATIVEID";
    public static final String SUSPENDED_USER_INITIATIVEID = INITIATIVEID + "S";
    private final int csvMaxRows = 100;

    @Mock private RewardsNotificationRepository rewardsNotificationRepositoryMock;
    @Mock private RewardNotification2ExportCsvService reward2CsvLineServiceMock;
    @Mock private Initiative2ExportRetrieverService initiative2ExportRetrieverServiceMock;
    @Mock private RewardOrganizationExportsRepository exportsRepositoryMock;
    @Mock private ExportCsvFinalizeService csvWriterServiceMock;
    @Mock private UserSuspensionService userSuspensionServiceMock;

    private ExportInitiativeRewardsService service;

    @BeforeEach
    void init() {
        service = new ExportInitiativeRewardsServiceImpl(1000, 50, csvMaxRows, rewardsNotificationRepositoryMock, reward2CsvLineServiceMock, initiative2ExportRetrieverServiceMock, exportsRepositoryMock, csvWriterServiceMock, userSuspensionServiceMock);
        Mockito.when(userSuspensionServiceMock.isNotSuspendedUser(Mockito.eq(INITIATIVEID), Mockito.anyString())).thenReturn(Mono.just(Boolean.TRUE));
    }

    @AfterEach
    void verifyNoMoreMockInvocations() {
        Mockito.verifyNoMoreInteractions(rewardsNotificationRepositoryMock, reward2CsvLineServiceMock, exportsRepositoryMock, csvWriterServiceMock);
    }

    @Test
    void testStuckExport() {
        // Given
        int stuckRewards = csvMaxRows - 10;
        int newRewards = csvMaxRows + 5;

        RewardOrganizationExport stuckExport = new RewardOrganizationExport();
        stuckExport.setId("STUCKEXPORTID");
        stuckExport.setInitiativeId(INITIATIVEID);
        stuckExport.setNotificationDate(LocalDate.now().minusDays(7));
        stuckExport.setExportDate(LocalDate.now());
        stuckExport.setProgressive(0L);

        List<RewardsNotification> stuckRewardNotification = IntStream.range(0, stuckRewards)
                .mapToObj(i -> RewardsNotificationFaker.mockInstanceBuilder(i)
                        .id("STUCKREWARDS%d".formatted(i))
                        .externalId("STUCKREWARDSEXTERNALID%d".formatted(i))
                        .exportId(stuckExport.getId())
                        .status(RewardNotificationStatus.EXPORTED)
                        .build())
                .peek(this::mockCsvLineBuild)
                .toList();
        Mockito.when(rewardsNotificationRepositoryMock.findExportRewards(stuckExport.getId())).thenReturn(Flux.fromIterable(stuckRewardNotification));

        Pair<List<RewardsNotification>, List<String>> rewardNotification2Notify_2_writingCsvEvents = mockCommonInvocations(stuckRewards, newRewards, stuckExport, 2);
        List<RewardsNotification> rewardNotification2Notify = rewardNotification2Notify_2_writingCsvEvents.getKey();
        RewardOrganizationExport exportSplit2 = mockNextSplitInvocation(stuckExport);

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

            // Not necessary the first lines should be (in any order) the stuck rewards: new rewards could occur in between
            Map<Boolean, Set<String>> csvLinesGroupByStuckCondition = csvLines.stream()
                    .map(RewardNotificationExportCsvDto::getUniqueID)
                    .collect(Collectors.groupingBy(
                            l->l.startsWith("STUCKREWARDSEXTERNALID"),
                            Collectors.toSet()));
            Assertions.assertEquals(
                    stuckRewardNotification.stream().map(RewardsNotification::getExternalId).collect(Collectors.toSet()),
                    csvLinesGroupByStuckCondition.get(true)
            );

            // the remaining rows are new records
            newRewardIdsExported.addAll(csvLinesGroupByStuckCondition.get(false));
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
        }), Mockito.same(exportSplit2));

        Assertions.assertEquals(
                rewardNotification2Notify.stream().map(RewardsNotification::getExternalId).collect(Collectors.toSet()),
                newRewardIdsExported);

        Assertions.assertEquals(
                List.of("writing export STUCKEXPORTID", "export STUCKEXPORTID completed", "writing export EXPORTID.1", "export EXPORTID.1 completed"),
                rewardNotification2Notify_2_writingCsvEvents.getValue());
    }

    @Test
    void testNotStuckExportAnd2SplitSameSize() {
        // Given
        int newRewards = csvMaxRows *2;

        RewardOrganizationExport export = new RewardOrganizationExport();
        export.setId("EXPORTID");
        export.setInitiativeId(INITIATIVEID);
        export.setNotificationDate(LocalDate.now());
        export.setProgressive(0L);
        export.setExportDate(LocalDate.now());

        Mockito.when(rewardsNotificationRepositoryMock.findExportRewards(export.getId())).thenReturn(Flux.empty());

        Pair<List<RewardsNotification>, List<String>> rewardNotification2Notify_2_writingCsvEvents = mockCommonInvocations(0, newRewards, export, 3);
        List<RewardsNotification> rewardNotification2Notify = rewardNotification2Notify_2_writingCsvEvents.getKey();
        RewardOrganizationExport exportSplit2 = mockNextSplitInvocation(export);

        // When
        List<RewardOrganizationExport> result = service.performExport(export, false).collectList().block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size()); // expected 2 split

        // new Ids to be verified
        Set<String> newRewardIdsExported = new HashSet<>();

        ArgumentMatcher<List<RewardNotificationExportCsvDto>> csvWriteMockedInvocation = csvLines -> {
            Assertions.assertEquals(csvMaxRows, csvLines.size());
            newRewardIdsExported.addAll(csvLines.stream().map(RewardNotificationExportCsvDto::getUniqueID).toList());
            return true;
        };
        Mockito.verify(csvWriterServiceMock).writeCsvAndFinalize(Mockito.argThat(csvWriteMockedInvocation), Mockito.same(export));
        Mockito.verify(csvWriterServiceMock).writeCsvAndFinalize(Mockito.argThat(csvWriteMockedInvocation), Mockito.same(exportSplit2));

        Assertions.assertEquals(
                rewardNotification2Notify.stream().map(RewardsNotification::getExternalId).collect(Collectors.toSet()),
                newRewardIdsExported);

        Assertions.assertEquals(
                List.of("writing export EXPORTID", "export EXPORTID completed", "writing export EXPORTID.1", "export EXPORTID.1 completed"),
                rewardNotification2Notify_2_writingCsvEvents.getValue());
    }

    private Pair<List<RewardsNotification>, List<String>> mockCommonInvocations(int baseIndex, int n, RewardOrganizationExport export, int maxProgressive) {
        List<RewardsNotification> rewardNotification2Notify = mockRewards(baseIndex, n, export);

        rewardNotification2Notify.forEach(this::mockCsvLineBuild);

        List<String> invocationOrder = new BlockingArrayQueue<>();
        Mockito.when(csvWriterServiceMock.writeCsvAndFinalize(Mockito.any(), Mockito.any())).thenAnswer(i -> {
            RewardOrganizationExport invocationExport = i.getArgument(1, RewardOrganizationExport.class);
            invocationOrder.add("writing export %s".formatted(invocationExport.getId()));
            return Mono.fromSupplier(() ->{
                // first splits will wait more time
                TestUtils.wait((maxProgressive-invocationExport.getProgressive())*10, TimeUnit.MILLISECONDS);
                invocationOrder.add("export %s completed".formatted(invocationExport.getId()));
                return invocationExport;
            });
        });
        return Pair.of(rewardNotification2Notify, invocationOrder);
    }

    private void mockCsvLineBuild(RewardsNotification r) {
        Mockito.when(reward2CsvLineServiceMock.apply(Mockito.same(r))).thenAnswer(i ->
                Mono.just(RewardNotificationExportCsvDto.builder()
                        .uniqueID(r.getExternalId())
                        .id(r.getId()).build()));
    }

    private List<RewardsNotification> mockRewards(int baseIndex, int n, RewardOrganizationExport export) {
        List<RewardsNotification> rewardNotification2Notify = buildMockRewardInstances(baseIndex, n);
        Mockito.when(rewardsNotificationRepositoryMock.findRewards2Notify(export.getInitiativeId(), export.getNotificationDate())).thenReturn(Flux.fromIterable(rewardNotification2Notify));
        return rewardNotification2Notify;
    }

    private List<RewardsNotification> mockRewards(int baseIndex, int n, RewardOrganizationExport export, boolean withSuspended) {
        List<RewardsNotification> rewardNotification2Notify = buildMockRewardInstances(baseIndex, n);

        if (withSuspended) {

            List<RewardsNotification> rewardNotification2NotifyWithSuspended = new ArrayList<>();
            rewardNotification2NotifyWithSuspended.addAll(rewardNotification2Notify);
            rewardNotification2NotifyWithSuspended.addAll(buildMockRewardInstancesSuspended(baseIndex+n, n));

            Mockito.when(rewardsNotificationRepositoryMock.findRewards2Notify(export.getInitiativeId(), export.getNotificationDate())).thenReturn(Flux.fromIterable(rewardNotification2NotifyWithSuspended));

            return rewardNotification2NotifyWithSuspended;
        }

        Mockito.when(rewardsNotificationRepositoryMock.findRewards2Notify(export.getInitiativeId(), export.getNotificationDate())).thenReturn(Flux.fromIterable(rewardNotification2Notify));
        return rewardNotification2Notify;
    }

    private static List<RewardsNotification> buildMockRewardInstances(int baseIndex, int n) {
        return IntStream.range(baseIndex, baseIndex + n).mapToObj(RewardsNotificationFaker::mockInstance).toList();
    }

    private static List<RewardsNotification> buildMockRewardInstancesSuspended(int baseIndex, int n) {
        return IntStream.range(baseIndex, baseIndex + n).mapToObj(i ->
                RewardsNotificationFaker.mockInstanceBuilder(i)
                        .initiativeId(SUSPENDED_USER_INITIATIVEID)
                        .build()
        ).toList();
    }

    private RewardOrganizationExport mockNextSplitInvocation(RewardOrganizationExport export) {
        RewardOrganizationExport exportSplit2 = new RewardOrganizationExport();
        exportSplit2.setId("EXPORTID.1");
        exportSplit2.setInitiativeId(INITIATIVEID);
        exportSplit2.setProgressive(1L);
        exportSplit2.setExportDate(LocalDate.now());

        Mockito.when(initiative2ExportRetrieverServiceMock.reserveNextSplitExport(Mockito.same(export), Mockito.eq(1))).thenReturn(Mono.just(exportSplit2));
        return exportSplit2;
    }

    @Test
    void testEmptyExport(){
        // Given
        int newRewards = csvMaxRows *2;

        RewardOrganizationExport export = new RewardOrganizationExport();
        export.setId("EXPORTID");
        export.setInitiativeId(INITIATIVEID);
        export.setNotificationDate(LocalDate.now());
        export.setProgressive(0L);
        export.setExportDate(LocalDate.now());

        Mockito.when(rewardsNotificationRepositoryMock.findExportRewards(export.getId())).thenReturn(Flux.empty());
        Mockito.when(exportsRepositoryMock.delete(Mockito.same(export))).thenReturn(Mono.empty());

        mockRewards(0, newRewards, export);
        Mockito.when(reward2CsvLineServiceMock.apply(Mockito.any())).thenReturn(Mono.empty());
        // When
        List<RewardOrganizationExport> result = service.performExport(export, false).collectList().block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(RewardOrganizationExportStatus.SKIPPED, result.get(0).getStatus());

        Mockito.verify(csvWriterServiceMock, Mockito.never()).writeCsvAndFinalize(Mockito.any(), Mockito.any());
    }

    @Test
    void testEmptyExportSplit(){
        // Given
        RewardOrganizationExport export = new RewardOrganizationExport();
        export.setId("EXPORTID");
        export.setInitiativeId(INITIATIVEID);
        export.setNotificationDate(LocalDate.now());
        export.setProgressive(0L);
        export.setExportDate(LocalDate.now());

        Mockito.when(rewardsNotificationRepositoryMock.findExportRewards(export.getId())).thenReturn(Flux.empty());

        Mockito.when(reward2CsvLineServiceMock.apply(Mockito.any())).thenReturn(Mono.empty());

        List<RewardsNotification> firstSplitRecords = mockCommonInvocations(0, csvMaxRows, export, 1).getKey();
        List<RewardsNotification> recordsDiscarded = buildMockRewardInstances(csvMaxRows, csvMaxRows * 2);

        Mockito.when(rewardsNotificationRepositoryMock.findRewards2Notify(export.getInitiativeId(), export.getNotificationDate())).thenReturn(Flux.fromStream(Stream.concat(firstSplitRecords.stream(), recordsDiscarded.stream())));

        // When
        List<RewardOrganizationExport> result = service.performExport(export, false).collectList().block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());

        Assertions.assertSame(export, result.get(0));
    }

    @Test
    void testWithSuspendedUser(){
        // Given
        Mockito.when(userSuspensionServiceMock.isNotSuspendedUser(Mockito.eq(SUSPENDED_USER_INITIATIVEID), Mockito.anyString())).thenReturn(Mono.just(Boolean.FALSE));
        Mockito.when(rewardsNotificationRepositoryMock.save(Mockito.any(RewardsNotification.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

        int newRewards = csvMaxRows *2;

        RewardOrganizationExport export = new RewardOrganizationExport();
        export.setId("EXPORTID");
        export.setInitiativeId(SUSPENDED_USER_INITIATIVEID);
        export.setNotificationDate(LocalDate.now());
        export.setProgressive(0L);
        export.setExportDate(LocalDate.now());

        Mockito.when(rewardsNotificationRepositoryMock.findExportRewards(export.getId())).thenReturn(Flux.empty());
        Mockito.when(exportsRepositoryMock.delete(Mockito.same(export))).thenReturn(Mono.empty());

        mockRewards(0, newRewards, export, true);
        Mockito.when(reward2CsvLineServiceMock.apply(Mockito.any())).thenReturn(Mono.empty());

        // When
        List<RewardOrganizationExport> result = service.performExport(export, false).collectList().block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(RewardOrganizationExportStatus.SKIPPED, result.get(0).getStatus());

        Mockito.verify(csvWriterServiceMock, Mockito.never()).writeCsvAndFinalize(Mockito.any(), Mockito.any());
    }
}
