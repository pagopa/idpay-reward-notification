package it.gov.pagopa.reward.notification.service.exports;

import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationExportsRepository;
import it.gov.pagopa.reward.notification.service.csv.out.ExportRewardNotificationCsvService;
import it.gov.pagopa.reward.notification.test.fakers.RewardOrganizationExportsFaker;
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

@ExtendWith(MockitoExtension.class)
class ForceOrganizationExportServiceImplTest {

    @Mock
    private RewardOrganizationExportsRepository repositoryMock;
    @Mock
    private ExportRewardNotificationCsvService csvServiceMock;

    private ForceOrganizationExportService service;

    @BeforeEach
    void init() {
        service = new ForceOrganizationExportServiceImpl(repositoryMock, csvServiceMock);
    }

    @Test
    void test() {
        LocalDate now = LocalDate.now();

        RewardOrganizationExport export1 = RewardOrganizationExportsFaker.mockInstanceBuilder(1)
                .exportDate(now)
                .build();
        RewardOrganizationExport export2 = RewardOrganizationExportsFaker.mockInstanceBuilder(2)
                .exportDate(now)
                .build();

        List<RewardOrganizationExport> exports = List.of(export1, export2);

        Mockito.when(repositoryMock.findByExportDate(now)).thenReturn(Flux.fromIterable(exports));
        Mockito.when(repositoryMock.save(Mockito.any(RewardOrganizationExport.class)))
                .thenAnswer(i -> Mono.just(i.getArgument(0, RewardOrganizationExport.class)));

        Mockito.when(csvServiceMock.execute(Mockito.any(LocalDate.class))).thenReturn(Flux.just(exports));

        List<RewardOrganizationExport> result = service.forceExecute(now).blockLast();

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isEmpty());

        for (int i = 0; i < result.size(); i++) {
            RewardOrganizationExport exportFromFaker = exports.get(i);
            RewardOrganizationExport exportFromResult = result.get(i);

            Assertions.assertEquals(LocalDate.now().minusDays(1), exportFromFaker.getExportDate());
            Assertions.assertEquals(exportFromFaker, exportFromResult);
        }

        Mockito.verify(csvServiceMock).execute(Mockito.any(LocalDate.class));
    }

}