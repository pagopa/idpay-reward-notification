package it.gov.pagopa.reward.notification.service.csv.out;

import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.service.csv.out.retrieve.Initiative2ExportRetrieverService;
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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ExportRewardNotificationCsvServiceTest {

    @Mock private Initiative2ExportRetrieverService initiative2ExportRetrieverServiceMock;
    @Mock private ExportInitiativeRewardsService exportInitiativeRewardsServiceMock;

    private ExportRewardNotificationCsvServiceImpl service;

    @BeforeEach
    void init() {
        service = Mockito.spy(new ExportRewardNotificationCsvServiceImpl(initiative2ExportRetrieverServiceMock, exportInitiativeRewardsServiceMock));
    }

    @Test
    void scheduleTest() {
        Mockito.doReturn(Flux.empty()).when(service).execute();

        service.schedule();

        Mockito.verify(service).execute();
    }

    @Test
    void executeTest() {
        // Given
        AtomicInteger times = new AtomicInteger(0);
        int N_STUCKS = 3;
        int N_NEW = 5;
        List<String> expectedInitiativeExported =
                Stream.concat(
                        IntStream.range(0, N_STUCKS).mapToObj("STUCKEXPORT_INITIATIVEID%d"::formatted),
                        IntStream.range(N_STUCKS, N_NEW+N_STUCKS).mapToObj("INITIATIVEID%d"::formatted)
                ).toList();

        Mockito.doAnswer(i ->
                        Mono.defer(() -> {
                            int x = times.getAndUpdate(prev -> prev < N_STUCKS ? prev + 1 : prev);
                            return x < N_STUCKS
                                    ? Mono.just(RewardOrganizationExport.builder()
                                    .initiativeId("STUCKEXPORT_INITIATIVEID%d".formatted(x))
                                    .notificationDate(LocalDate.now().minusDays(x + 1))
                                    .exportDate(LocalDate.now())
                                    .build())
                                    : Mono.empty();
                        }))
                .when(initiative2ExportRetrieverServiceMock).retrieveStuckExecution();

        Mockito.doAnswer(i ->
                        Mono.defer(() -> {
                            int x = times.getAndIncrement();
                            return x < N_STUCKS + N_NEW
                                    ? Mono.just(RewardOrganizationExport.builder()
                                    .initiativeId("INITIATIVEID%d".formatted(x))
                                    .notificationDate(LocalDate.now())
                                    .exportDate(LocalDate.now())
                                    .build())
                                    : Mono.empty();
                        }))
                .when(initiative2ExportRetrieverServiceMock).retrieve();

        Mockito.doAnswer(i->Flux.just(i.getArgument(0,RewardOrganizationExport.class)))
                .when(exportInitiativeRewardsServiceMock)
                .performExport(Mockito.any(), Mockito.anyBoolean());

        // When
        List<RewardOrganizationExport> result = Objects.requireNonNull(service.execute().collectList().block()).stream().flatMap(List::stream).toList();
        Assertions.assertNotNull(result);

        Assertions.assertEquals(
                expectedInitiativeExported,
                result.stream().map(RewardOrganizationExport::getInitiativeId).toList());

        Assertions.assertEquals(N_STUCKS+N_NEW+1, times.get());

        Assertions.assertEquals(
                expectedInitiativeExported,
                Mockito.mockingDetails(exportInitiativeRewardsServiceMock).getInvocations().stream().map(i->i.getArgument(0, RewardOrganizationExport.class).getInitiativeId()).toList()
        );

        Mockito.verify(exportInitiativeRewardsServiceMock, Mockito.times(N_STUCKS)).performExport(Mockito.any(), Mockito.eq(true));
        Mockito.verify(exportInitiativeRewardsServiceMock, Mockito.times(N_NEW)).performExport(Mockito.any(), Mockito.eq(false));

        Mockito.verifyNoMoreInteractions(initiative2ExportRetrieverServiceMock, exportInitiativeRewardsServiceMock);
    }
}
