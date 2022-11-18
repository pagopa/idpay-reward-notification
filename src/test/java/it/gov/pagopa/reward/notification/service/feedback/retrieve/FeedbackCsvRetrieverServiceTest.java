package it.gov.pagopa.reward.notification.service.feedback.retrieve;

import com.azure.core.http.rest.Response;
import it.gov.pagopa.reward.notification.connector.azure.storage.RewardsNotificationBlobClient;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.test.fakers.RewardOrganizationImportFaker;
import it.gov.pagopa.reward.notification.utils.RewardFeedbackConstants;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FeedbackCsvRetrieverServiceTest {

    @Mock
    private RewardsNotificationBlobClient blobClientMock;

    private FeedbackCsvRetrieverService service;

    private final RewardOrganizationImport importRequest = RewardOrganizationImportFaker.mockInstance(0);
    private static final String csvTmpDir = "target/tmp/feedbackUseCasesZip";

    @BeforeAll
    static void copySampleToTargetDir() throws IOException {
        String srcDir = "src/test/resources/feedbackUseCasesZip";
        Files.createDirectories(Path.of(csvTmpDir));
        try(Stream<Path> pathStream = Files.walk(Paths.get(srcDir))) {
            pathStream.forEach(source -> {
                Path destination = Paths.get(csvTmpDir, source.toString()
                        .substring(srcDir.length()));
                try {
                    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    @AfterAll
    static void checkSampleTargetDir() throws IOException {
        Path sampleTmpDir = Path.of(csvTmpDir);
        try(Stream<Path> fileListStream = Files.list(sampleTmpDir)){
            Assertions.assertEquals(
                    List.of(
                            sampleTmpDir.resolve("invalid"),
                            sampleTmpDir.resolve("valid")
                            ),
                    fileListStream.toList()
            );
        } finally {
            clearSampleTargetDir(sampleTmpDir);
        }
    }

    private static void clearSampleTargetDir(Path sampleTmpDir) throws IOException {
        try(Stream<Path> pathStream = Files.walk(sampleTmpDir)) {
            pathStream.forEach(source -> {
                try {
                    Files.delete(source);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        Files.delete(sampleTmpDir);
    }

    @BeforeEach
    void init() {
        service = new FeedbackCsvRetrieverServiceImpl(csvTmpDir, ";", blobClientMock);

        //noinspection unchecked
        Mockito.when(blobClientMock.downloadFile(Mockito.anyString(), Mockito.any())).thenReturn(Mono.just(Mockito.mock(Response.class)));
    }

    @AfterEach
    void verifyNotMoreMocksInteraction() throws IOException {
        Path expectedZipLocalPath = Path.of(csvTmpDir, importRequest.getFilePath());

        Files.deleteIfExists(buildExpectedCsvLocalPath());

        Mockito.verify(blobClientMock)
                .downloadFile(importRequest.getFilePath(), expectedZipLocalPath);
        Mockito.verifyNoMoreInteractions(blobClientMock);
    }

    private Path buildExpectedCsvLocalPath() {
        return Path.of(csvTmpDir, importRequest.getFilePath().replace(".zip", ".csv"));
    }

    @Test
    void testLocalZipNotExistent() {
        Mono<Path> mono = service.retrieveCsv(importRequest);
        try {
            mono.block();
            Assertions.fail("Expecting exception when no zip exists");
        } catch (IllegalStateException e) {
            Assertions.assertEquals("[REWARD_NOTIFICATION_FEEDBACK] Something gone wrong while handling local zipFile target\\tmp\\feedbackUseCasesZip\\orgId\\initiativeId\\import\\reward-dispositive-0.zip", e.getMessage());
        }
    }

    @Test
    void testZipNoSize() {
        // Given
        importRequest.setFilePath("invalid/noSizeZip.zip");

        // When
        Path result = service.retrieveCsv(importRequest).block();

        // Then
        checkInvalidZip(result, RewardFeedbackConstants.ImportFileErrors.NO_SIZE);
    }

    @Test
    void testZipEmpty() {
        // Given
        importRequest.setFilePath("invalid/emptyZip.zip");

        // When
        Path result = service.retrieveCsv(importRequest).block();

        // Then
        checkInvalidZip(result, RewardFeedbackConstants.ImportFileErrors.EMPTY_ZIP);
    }

    @Test
    void testZipInvalidContent() {
        // Given
        importRequest.setFilePath("invalid/invalidContent.zip");

        // When
        Path result = service.retrieveCsv(importRequest).block();

        // Then
        checkInvalidZip(result, RewardFeedbackConstants.ImportFileErrors.INVALID_CONTENT);
    }

    @Test
    void testZipInvalidCsvName() {
        // Given
        importRequest.setFilePath("invalid/invalidCsvName.zip");

        // When
        Path result = service.retrieveCsv(importRequest).block();

        // Then
        checkInvalidZip(result, RewardFeedbackConstants.ImportFileErrors.INVALID_CSV_NAME);
    }

    @Test
    void testZipInvalidHeader() {
        // Given
        importRequest.setFilePath("invalid/invalidHeader.zip");

        // When
        Path result = service.retrieveCsv(importRequest).block();

        // Then
        checkInvalidZip(result, RewardFeedbackConstants.ImportFileErrors.INVALID_HEADERS);
    }

    @Test
    void testSuccessful() {
        // Given
        importRequest.setFilePath("valid/validUseCase.zip");
        Path expectedCsvPath = buildExpectedCsvLocalPath();

        // When
        Path result = service.retrieveCsv(importRequest).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectedCsvPath, result);
    }

    private void checkInvalidZip(Path result, RewardFeedbackConstants.ImportFileErrors expectedError) {
        Assertions.assertNull(result);
        Assertions.assertEquals(List.of(new RewardOrganizationImport.RewardOrganizationImportError(expectedError)), importRequest.getErrors());
    }
}
