package it.gov.pagopa.reward.notification.service.csv.export;

import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ExportCsvServiceTest {
    @Mock private Initiative2ExportRetrieverService initiative2ExportRetrieverServiceMock;

    private ExportCsvServiceImpl service;

    @BeforeEach
    void init(){
        service = Mockito.spy(new ExportCsvServiceImpl(initiative2ExportRetrieverServiceMock));
    }

    @Test
    void scheduleTest(){
        Mockito.doReturn(Mono.empty()).when(service).execute();

        service.schedule();

        Mockito.verify(service).execute();
    }

    @Test
    void executeTest(){
        // Given
        Mockito.doReturn(Mono.just(new RewardOrganizationExport())).when(initiative2ExportRetrieverServiceMock).retrieve();

        // When
        service.execute().block();

        Mockito.verify(initiative2ExportRetrieverServiceMock).retrieve();
    }
}
