package it.gov.pagopa.reward.notification.service.csv.in;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationImportCsvDto;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationImportResult;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.service.csv.in.retrieve.RewardNotificationExportFeedbackRetrieverService;
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
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

        service = new ImportRewardNotificationFeedbackCsvServiceImpl(';', 7, rowHandlerServiceMock, exportFeedbackRetrieverServiceMock);

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

        List<String> expectedExportIds = List.of("EXPORTID0", "EXPORTID1", "EXPORTID2");

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
            return Mono.just(new RewardNotificationFeedbackHandlerOutcome(feedbackOutcome, error==null? "EXPORTID%d".formatted(row.getRowNumber()%3) : null, error));
        });

        Mockito.when(exportFeedbackRetrieverServiceMock.updateExportStatus(expectedExportIds))
                .thenReturn(Flux.just(Mockito.mock(UpdateResult.class), Mockito.mock(UpdateResult.class)));

        // When
        RewardOrganizationImport result = service.evaluate(sampleCsv, importRequest).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(
                expectedExportIds,
                result.getExportIds());

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
    }

    private RewardOrganizationImport.RewardOrganizationImportError buildDummyError(Integer rowNumber) {
        return new RewardOrganizationImport.RewardOrganizationImportError(rowNumber, "DUMMYERROR", "DUMMYERRORREASON");
    }
}
