package it.gov.pagopa.reward.notification.service.csv.in;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationImportCsvDto;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationImportResult;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.service.csv.in.retrieve.RewardNotificationExportFeedbackRetrieverService;
import it.gov.pagopa.reward.notification.service.csv.in.utils.RewardNotificationFeedbackExportDelta;
import it.gov.pagopa.reward.notification.service.csv.in.utils.RewardNotificationFeedbackHandlerOutcome;
import it.gov.pagopa.reward.notification.utils.ZipUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ExtendWith(MockitoExtension.class)
class ImportRewardNotificationFeedbackCsvServiceTest {

    @Mock private RewardNotificationFeedbackHandlerService rowHandlerServiceMock;
    @Mock private RewardNotificationExportFeedbackRetrieverService exportFeedbackRetrieverServiceMock;

    private ImportRewardNotificationFeedbackCsvService service;

    private final Path sampleCsv=Path.of("target/tmp/feedbackUseCasesZip/validUseCase.csv");

    @BeforeEach
    void init(){
        ((Logger) LoggerFactory.getLogger("org.apache.commons.beanutils.converters")).setLevel(Level.OFF);
        ((Logger) LoggerFactory.getLogger("org.apache.commons.beanutils.ConvertUtils")).setLevel(Level.OFF);

        service = new ImportRewardNotificationFeedbackCsvServiceImpl(';', rowHandlerServiceMock, exportFeedbackRetrieverServiceMock);

        ZipUtils.unzip("src/test/resources/feedbackUseCasesZip/valid/validUseCase.zip", sampleCsv.getParent().toString());
    }

    @AfterEach
    void checkCsvExistance() throws IOException {
        try{
            BaseIntegrationTest.waitFor(()->!Files.exists(sampleCsv), ()->"The local csv has not been deleted! %s".formatted(sampleCsv), 5, 500);
        } finally {
            Files.deleteIfExists(sampleCsv);
            Files.deleteIfExists(sampleCsv.getParent());
        }
    }

    @Test
    void test(){
        // Given
        RewardOrganizationImport importRequest = new RewardOrganizationImport();

        Set<Integer> simulatingErrorRows = Set.of(5,9,11,17); // row 9 is a sample of KO outcome, the other are OK

        List<RewardOrganizationExport> expectedExports = List.of(
                RewardOrganizationExport.builder()
                        .id("EXPORTID0")
                        .build(),
                RewardOrganizationExport.builder()
                        .id("EXPORTID1")
                        .build(),
                RewardOrganizationExport.builder()
                        .id("EXPORTID2")
                        .build());

        Map<String, RewardNotificationFeedbackExportDelta> expectedExportDeltas = expectedExports.stream()
                .collect(Collectors.toMap(RewardOrganizationExport::getId, export -> new RewardNotificationFeedbackExportDelta(export, 0L, 0L, 0L)));

        //noinspection unchecked
        Mockito.when(rowHandlerServiceMock.evaluate(Mockito.any(), Mockito.same(importRequest), Mockito.isA(ConcurrentHashMap.class))).thenAnswer(r-> {
            RewardNotificationImportCsvDto row = r.getArgument(0, RewardNotificationImportCsvDto.class);
            RewardOrganizationImportResult feedbackOutcome = RewardOrganizationImportResult.valueOf(row.getResult());
            RewardOrganizationImport.RewardOrganizationImportError error;
            if(simulatingErrorRows.contains(row.getRowNumber())){
                error= buildDummyError(row.getRowNumber());
            } else {
                error=null;
            }

            String exportId = "EXPORTID%d".formatted(row.getRowNumber() % 3);

            RewardNotificationFeedbackExportDelta rowResultedExportDelta = null;
            if(error == null){
                RewardNotificationFeedbackExportDelta exportRequestDelta = expectedExportDeltas.get(exportId);

                rowResultedExportDelta = new RewardNotificationFeedbackExportDelta(exportRequestDelta.getExport(), 1L, RewardOrganizationImportResult.OK.equals(feedbackOutcome) ? 1 : 0, RewardOrganizationImportResult.OK.equals(feedbackOutcome) ? 10L : 0L);

                synchronized(expectedExportDeltas){
                    expectedExportDeltas.put(exportId, RewardNotificationFeedbackExportDelta.add(exportRequestDelta, rowResultedExportDelta));
                }
            }

            return Mono.just(new RewardNotificationFeedbackHandlerOutcome(feedbackOutcome, error, rowResultedExportDelta));
        });

        Mockito.when(exportFeedbackRetrieverServiceMock.updateCounters(Mockito.any())).thenReturn(Mono.just(Mockito.mock(UpdateResult.class)));

        Mockito.when(exportFeedbackRetrieverServiceMock.updateExportStatus(expectedExportDeltas.keySet()))
                .thenReturn(Flux.just(Mockito.mock(UpdateResult.class), Mockito.mock(UpdateResult.class)));

        // When
        RewardOrganizationImport result = service.evaluate(sampleCsv, importRequest).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(
                expectedExportDeltas.keySet(),
                new HashSet<>(result.getExportIds()));

        Assertions.assertEquals(17, result.getRewardsResulted());
        Assertions.assertEquals(4, result.getRewardsResultedError());
        Assertions.assertEquals(14, result.getRewardsResultedOk());
        Assertions.assertEquals(3, result.getRewardsResultedOkError());

        Assertions.assertEquals(7647, result.getPercentageResulted());
        Assertions.assertEquals(8235, result.getPercentageResultedOk());
        Assertions.assertEquals(8461, result.getPercentageResultedOkElab());

        Assertions.assertEquals(
                simulatingErrorRows.stream().sorted().map(this::buildDummyError).toList(),
                result.getErrors()
        );

        //noinspection unchecked
        Mockito.verify(rowHandlerServiceMock, Mockito.times(result.getRewardsResulted().intValue())).evaluate(Mockito.any(), Mockito.same(importRequest), Mockito.isA(ConcurrentHashMap.class));
        //noinspection unchecked
        IntStream.rangeClosed(1, 17)
                .forEach(i -> Mockito.verify(rowHandlerServiceMock).evaluate(Mockito.argThat(r -> {
                            if (r.getRowNumber() == i) {
                                Assertions.assertEquals("rewardNotificationId%d".formatted(i), r.getUniqueID());
                                return true;
                            } else {
                                return false;
                            }
                        }),
                        Mockito.same(importRequest), Mockito.isA(ConcurrentHashMap.class)));

        expectedExportDeltas.forEach((exportId, exportDelta) ->
                Mockito.verify(exportFeedbackRetrieverServiceMock).updateCounters(exportDelta));
    }

    private RewardOrganizationImport.RewardOrganizationImportError buildDummyError(Integer rowNumber) {
        return new RewardOrganizationImport.RewardOrganizationImportError(rowNumber, "DUMMYERROR", "DUMMYERRORREASON");
    }
}
