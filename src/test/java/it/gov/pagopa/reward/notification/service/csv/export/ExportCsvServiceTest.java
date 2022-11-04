package it.gov.pagopa.reward.notification.service.csv.export;

import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@ExtendWith(MockitoExtension.class)
class ExportCsvServiceTest {
    @Mock
    private Initiative2ExportRetrieverService initiative2ExportRetrieverServiceMock;

    private ExportCsvServiceImpl service;

    @BeforeEach
    void init() {
        service = Mockito.spy(new ExportCsvServiceImpl(initiative2ExportRetrieverServiceMock));
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
        int N = 5;

        Mockito.doAnswer(i ->
                        Mono.defer(() -> {
                            int x = times.getAndIncrement();
                            return x < N
                                    ? Mono.just(RewardOrganizationExport.builder().initiativeId("INITIATIVEID%d".formatted(x)).build())
                                    : Mono.empty();
                        }))
                .when(initiative2ExportRetrieverServiceMock).retrieve();

        // When
        List<RewardOrganizationExport> result = service.execute().collectList().block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(
                IntStream.range(0, N).mapToObj("INITIATIVEID%d"::formatted).toList(),
                result.stream().map(RewardOrganizationExport::getInitiativeId).toList());

        Assertions.assertEquals(N+1, times.get());

        Mockito.verifyNoMoreInteractions(initiative2ExportRetrieverServiceMock);
    }
}
