package it.gov.pagopa.reward.notification.service.csv.export.writer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationExportCsvDto;
import it.gov.pagopa.reward.notification.enums.ExportStatus;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationExportsRepository;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.IntStream;

@ExtendWith(MockitoExtension.class)
class ExportCsvFinalizeServiceTest {

    @Mock private RewardsNotificationRepository rewardsNotificationRepositoryMock;
    @Mock private RewardOrganizationExportsRepository rewardOrganizationExportsRepositoryMock;

    private ExportCsvFinalizeService service;

    @BeforeEach
    void init(){
        ((Logger) LoggerFactory.getLogger("org.apache.commons.beanutils.converters")).setLevel(Level.OFF);
        char csvSeparator = ';';
        service = new ExportCsvFinalizeServiceImpl(csvSeparator, rewardsNotificationRepositoryMock, rewardOrganizationExportsRepositoryMock);
    }

    @Test
    void test(){// TODO test csv write
        // Given
        List<RewardNotificationExportCsvDto> csvLines = IntStream.range(0, 10)
                .mapToObj(i -> RewardNotificationExportCsvDto.builder()
                        .uniqueID("REWARDNOTIFICATIONID%d".formatted(i))
                        .iban("IBAN%d".formatted(i))
                        .checkIban("CHECKIBAN%d".formatted(i))
                        .amount(100L)
                        .build()
                ).toList();

        RewardOrganizationExport export = RewardOrganizationExport.builder().id("EXPORTID").build();

        csvLines.forEach(l ->
                        Mockito.when(rewardsNotificationRepositoryMock.updateExportStatus(l.getUniqueID(), l.getIban(), l.getCheckIban(), "EXPORTID"))
                                .thenReturn(Mono.just(Mockito.mock(UpdateResult.class)))
                );

        Mockito.when(rewardOrganizationExportsRepositoryMock.save(Mockito.same(export))).thenReturn(Mono.just(export));

        // When
        RewardOrganizationExport result = service.writeCsvAndFinalize(csvLines, export).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertSame(export, result);

        Assertions.assertEquals(ExportStatus.EXPORTED, result.getStatus());
        Assertions.assertEquals(10, result.getRewardNotified());
        Assertions.assertEquals(1000L, result.getRewardsExportedCents());

        Mockito.verifyNoMoreInteractions(rewardsNotificationRepositoryMock, rewardOrganizationExportsRepositoryMock);
    }
}
