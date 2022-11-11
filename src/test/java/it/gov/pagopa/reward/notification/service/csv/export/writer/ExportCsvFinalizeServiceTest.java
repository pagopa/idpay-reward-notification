package it.gov.pagopa.reward.notification.service.csv.export.writer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.azure.core.http.rest.Response;
import com.azure.storage.blob.models.BlockBlobItem;
import com.mongodb.Function;
import it.gov.pagopa.reward.notification.azure.storage.RewardsNotificationBlobClient;
import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationExportCsvDto;
import it.gov.pagopa.reward.notification.enums.ExportStatus;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationExportsRepository;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.utils.ZipUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ExtendWith(MockitoExtension.class)
class ExportCsvFinalizeServiceTest {

    @Mock private RewardsNotificationRepository rewardsNotificationRepositoryMock;
    @Mock private RewardOrganizationExportsRepository rewardOrganizationExportsRepositoryMock;
    @Mock private RewardsNotificationBlobClient rewardsNotificationBlobClientMock;

    private ExportCsvFinalizeService service;

    @BeforeEach
    void init() {
        ((Logger) LoggerFactory.getLogger("org.apache.commons.beanutils.converters")).setLevel(Level.OFF);
        char csvSeparator = ';';
        service = new ExportCsvFinalizeServiceImpl(csvSeparator, rewardsNotificationRepositoryMock, rewardOrganizationExportsRepositoryMock, rewardsNotificationBlobClientMock);
    }

    @Test
    void test() throws IOException {
        // Given
        List<RewardNotificationExportCsvDto> csvLines = IntStream.range(0, 10)
                .mapToObj(i -> RewardNotificationExportCsvDto.builder()
                        .progressiveCode((long) i)
                        .id("REWARDNOTIFICATIONID%d".formatted(i))
                        .uniqueID("REWARDNOTIFICATIONEXTERNALID%d".formatted(i))
                        .fiscalCode("fiscalCode%d".formatted(i))
                        .accountHolderName("accountHolderName%d".formatted(i))
                        .accountHolderSurname("accountHolderSurname%d".formatted(i))
                        .iban("IBAN%d".formatted(i))
                        .amount(100L)
                        .paymentReason("paymentReason%d".formatted(i))
                        .initiativeName("initiativeName%d".formatted(i))
                        .initiativeID("initiativeID%d".formatted(i))
                        .startDatePeriod("startDatePeriod%d".formatted(i))
                        .endDatePeriod("endDatePeriod%d".formatted(i))
                        .organizationId("organizationId%d".formatted(i))
                        .organizationFiscalCode("organizationFiscalCode%d".formatted(i))
                        .checkIban("CHECKIBAN%d".formatted(i))
                        .typologyReward("typologyReward%d".formatted(i))

                        .build()
                ).toList();

        RewardOrganizationExport export = RewardOrganizationExport.builder()
                .id("EXPORTID")
                .filePath("/result.zip")
                .build();

        csvLines.forEach(l ->
                Mockito.when(rewardsNotificationRepositoryMock.updateExportStatus(l.getId(), l.getIban(), l.getCheckIban(), "EXPORTID"))
                        .thenAnswer(i->Mono.just(i.getArgument(0)))
        );

        Mockito.when(rewardOrganizationExportsRepositoryMock.save(Mockito.same(export))).thenReturn(Mono.just(export));

        File zipFile = new File("/tmp", export.getFilePath());
        Mockito.when(rewardsNotificationBlobClientMock.uploadFile(zipFile, export.getFilePath(), "application/zip"))
                .thenAnswer(i->{
                    Path zipPath = Path.of(zipFile.getAbsolutePath());
                    Files.copy(zipPath,
                            zipPath.getParent().resolve(zipPath.getFileName().toString().replace(".zip", ".uploaded.zip")),
                            StandardCopyOption.REPLACE_EXISTING);
                    //noinspection rawtypes
                    Response responseMocked = Mockito.mock(Response.class);
                    Mockito.when(responseMocked.getStatusCode()).thenReturn(201);
                    return Mono.just(responseMocked);
                });

        // When
        RewardOrganizationExport result = service.writeCsvAndFinalize(csvLines, export).block();

        // Then
        checkExportFile(csvLines, export, result);

        // checking if a re-execution would result into error
        result = service.writeCsvAndFinalize(csvLines, export).block();
        checkExportFile(csvLines, export, result);

        Mockito.verifyNoMoreInteractions(rewardsNotificationRepositoryMock, rewardOrganizationExportsRepositoryMock);
    }

    private void checkExportFile(List<RewardNotificationExportCsvDto> csvLines, RewardOrganizationExport export, RewardOrganizationExport result) throws IOException {
        Assertions.assertNotNull(result);
        Assertions.assertSame(export, result);

        Assertions.assertEquals(ExportStatus.EXPORTED, result.getStatus());
        Assertions.assertEquals(10, result.getRewardNotified());
        Assertions.assertEquals(1000L, result.getRewardsExportedCents());

        Assertions.assertFalse(Files.exists(Paths.get("/tmp/result.zip")));
        Path zipPath = Paths.get("/tmp/result.uploaded.zip");
        Assertions.assertTrue(Files.exists(zipPath));
        Path csvPath = Paths.get("/tmp/result.csv");
        Assertions.assertFalse(Files.exists(csvPath));

        ZipUtils.unzip(zipPath.toString(), csvPath.getParent().toString());
        try {
            Assertions.assertTrue(Files.exists(csvPath));

            List<String> csvLinesStrs = Files.readAllLines(csvPath);
            Assertions.assertEquals(
                    "\"progressiveCode\";\"uniqueID\";\"fiscalCode\";\"accountHolderName\";\"accountHolderSurname\";\"iban\";\"amount\";\"paymentReason\";\"initiativeName\";\"initiativeID\";\"startDatePeriod\";\"endDatePeriod\";\"organizationId\";\"organizationFiscalCode\";\"checkIban\";\"typologyReward\";\"RelatedPaymentID\"",
                    csvLinesStrs.get(0));

            for (int i = 0; i < csvLines.size(); i++) {
                Assertions.assertEquals(
                        expectedCsvLine(csvLines.get(i)),
                        csvLinesStrs.get(i + 1));
            }
        }
        finally {
            Files.delete(csvPath);
        }
    }

    private final List<Function<RewardNotificationExportCsvDto, String>> cellGetters=List.of(
            e -> e.getProgressiveCode()+"",
            RewardNotificationExportCsvDto::getUniqueID,
            RewardNotificationExportCsvDto::getFiscalCode,
            RewardNotificationExportCsvDto::getAccountHolderName,
            RewardNotificationExportCsvDto::getAccountHolderSurname,
            RewardNotificationExportCsvDto::getIban,
            e -> e.getAmount()+"",
            RewardNotificationExportCsvDto::getPaymentReason,
            RewardNotificationExportCsvDto::getInitiativeName,
            RewardNotificationExportCsvDto::getInitiativeID,
            RewardNotificationExportCsvDto::getStartDatePeriod,
            RewardNotificationExportCsvDto::getEndDatePeriod,
            RewardNotificationExportCsvDto::getOrganizationId,
            RewardNotificationExportCsvDto::getOrganizationFiscalCode,
            RewardNotificationExportCsvDto::getCheckIban,
            RewardNotificationExportCsvDto::getTypologyReward,
            RewardNotificationExportCsvDto::getRelatedPaymentID
    );
    private String expectedCsvLine(RewardNotificationExportCsvDto lineDto) {
        return cellGetters.stream().map(g -> g.apply(lineDto)).map(v->"\"%s\"".formatted(ObjectUtils.firstNonNull(v,""))).collect(Collectors.joining(";"));
    }

    @Test
    void testUploadError(){
        // Given
        RewardOrganizationExport export = new RewardOrganizationExport();
        export.setFilePath("TESTFILE");
        RewardNotificationExportCsvDto reward = new RewardNotificationExportCsvDto();
        reward.setAmount(100L);

        @SuppressWarnings("unchecked") Response<BlockBlobItem> responseMocked = Mockito.mock(Response.class);
        Mockito.when(responseMocked.getStatusCode()).thenReturn(404);
        Mockito.when(rewardsNotificationBlobClientMock.uploadFile(new File("/tmp",export.getFilePath()), export.getFilePath(), "application/zip"))
                .thenReturn(Mono.just(responseMocked));

        // When
        RewardOrganizationExport result = service.writeCsvAndFinalize(List.of(reward), export).block();

        // Then
        Assertions.assertNull(result);
    }
}
