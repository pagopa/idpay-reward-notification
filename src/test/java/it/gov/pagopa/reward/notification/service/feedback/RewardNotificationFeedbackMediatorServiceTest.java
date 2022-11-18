package it.gov.pagopa.reward.notification.service.feedback;

import it.gov.pagopa.reward.notification.dto.StorageEventDto;
import it.gov.pagopa.reward.notification.dto.mapper.StorageEvent2OrganizationImportMapper;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationImportStatus;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationImportsRepository;
import it.gov.pagopa.reward.notification.service.ErrorNotifierService;
import it.gov.pagopa.reward.notification.service.LockService;
import it.gov.pagopa.reward.notification.service.csv.in.ImportRewardNotificationFeedbackCsvService;
import it.gov.pagopa.reward.notification.service.feedback.retrieve.FeedbackCsvRetrieverService;
import it.gov.pagopa.reward.notification.test.fakers.StorageEventDtoFaker;
import it.gov.pagopa.reward.notification.test.utils.TestUtils;
import it.gov.pagopa.reward.notification.utils.RewardFeedbackConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(MockitoExtension.class)
class RewardNotificationFeedbackMediatorServiceTest {

    private final StorageEvent2OrganizationImportMapper mapper = new StorageEvent2OrganizationImportMapper();
    @Mock private LockService lockServiceMock;
    @Mock private RewardOrganizationImportsRepository importsRepositoryMock;
    @Mock private FeedbackCsvRetrieverService csvRetrieverServiceMock;
    @Mock private ImportRewardNotificationFeedbackCsvService importRewardNotificationFeedbackCsvServiceMock;
    @Mock private ErrorNotifierService errorNotifierServiceMock;

    private RewardNotificationFeedbackMediatorServiceImpl feedbackMediatorService;

    @BeforeEach
    void init(){
        feedbackMediatorService = new RewardNotificationFeedbackMediatorServiceImpl("APPNAME", 500, mapper, lockServiceMock, importsRepositoryMock, csvRetrieverServiceMock, importRewardNotificationFeedbackCsvServiceMock, errorNotifierServiceMock, TestUtils.objectMapper);
    }

    @AfterEach
    void verifyNotMoreMocksInteraction(){
        Mockito.verifyNoMoreInteractions(lockServiceMock, importsRepositoryMock, csvRetrieverServiceMock, importRewardNotificationFeedbackCsvServiceMock, errorNotifierServiceMock);
    }

    @Test
    void testNoImportFile(){
        // Given
        StorageEventDto event = StorageEventDtoFaker.mockInstance(0);
        event.setSubject("NOT/IMPORT/FILE.ZIP");

        // When
        List<RewardOrganizationImport> result = feedbackMediatorService.execute(List.of(event), MessageBuilder.withPayload("").build(), new HashMap<>()).block();

        // Then
        checkEmptyResult(result);
    }

    @Test
    void testNoUploadEvent(){
        // Given
        StorageEventDto event = StorageEventDtoFaker.mockInstance(0);
        event.setEventType("Microsoft.Storage.BlobDeleted");

        // When
        List<RewardOrganizationImport> result = feedbackMediatorService.execute(List.of(event), MessageBuilder.withPayload("").build(), new HashMap<>()).block();

        // Then
        checkEmptyResult(result);
    }

    @Test
    void testRequestAlreadyElaborated(){
        // Given
        StorageEventDto event = StorageEventDtoFaker.mockInstance(0);
        RewardOrganizationImport expectedImportRequest = mapper.apply(event);

        Mockito.when(importsRepositoryMock.createIfNotExistsOrReturnEmpty(expectedImportRequest)).thenReturn(Mono.empty());

        // When
        List<RewardOrganizationImport> result = feedbackMediatorService.execute(List.of(event), MessageBuilder.withPayload("").build(), new HashMap<>()).block();

        // Then
        checkEmptyResult(result);
    }

    private void checkEmptyResult(List<RewardOrganizationImport> result) {
        Assertions.assertNotNull(result);
        Assertions.assertEquals(Collections.emptyList(), result);
    }

    @Test
    void testCannotRetrieveCsv(){
        // Given
        StorageEventDto event = StorageEventDtoFaker.mockInstance(0);
        RewardOrganizationImport expectedImportRequest = mapper.apply(event);

        Mockito.when(importsRepositoryMock.createIfNotExistsOrReturnEmpty(expectedImportRequest)).thenReturn(Mono.just(expectedImportRequest));
        Mockito.when(csvRetrieverServiceMock.retrieveCsv(expectedImportRequest)).thenAnswer(i->Mono.empty());
        Mockito.when(importsRepositoryMock.save(expectedImportRequest)).thenReturn(Mono.just(expectedImportRequest));

        // When
        List<RewardOrganizationImport> result = feedbackMediatorService.execute(List.of(event), MessageBuilder.withPayload("").build(), new HashMap<>()).block();

        // Then
        checkResult(result, expectedImportRequest, RewardOrganizationImportStatus.ERROR,
                List.of(new RewardOrganizationImport.RewardOrganizationImportError(RewardFeedbackConstants.ImportFileErrors.NO_ROWS)));
    }

    @Test
    void testNoRewardsElaborated(){
        // Given
        StorageEventDto event = StorageEventDtoFaker.mockInstance(0);
        RewardOrganizationImport expectedImportRequest = mapper.apply(event);
        Path expectedLocalCsvPath = Path.of("PATH/TO/LOCAL/CSV.csv");

        Mockito.when(importsRepositoryMock.createIfNotExistsOrReturnEmpty(expectedImportRequest)).thenReturn(Mono.just(expectedImportRequest));
        Mockito.when(csvRetrieverServiceMock.retrieveCsv(expectedImportRequest)).thenAnswer(i->Mono.just(expectedLocalCsvPath));
        Mockito.when(importRewardNotificationFeedbackCsvServiceMock.evaluate(expectedLocalCsvPath, expectedImportRequest)).thenReturn(Flux.empty());
        Mockito.when(importsRepositoryMock.save(expectedImportRequest)).thenReturn(Mono.just(expectedImportRequest));

        // When
        List<RewardOrganizationImport> result = feedbackMediatorService.execute(List.of(event), MessageBuilder.withPayload("").build(), new HashMap<>()).block();

        // Then
        checkResult(result, expectedImportRequest, RewardOrganizationImportStatus.ERROR,
                List.of(new RewardOrganizationImport.RewardOrganizationImportError(RewardFeedbackConstants.ImportFileErrors.NO_ROWS)));
    }

    @Test
    void testPartialElaboration(){
        // Given
        StorageEventDto event = StorageEventDtoFaker.mockInstance(0);
        RewardOrganizationImport expectedImportRequest = mapper.apply(event);
        Path expectedLocalCsvPath = Path.of("PATH/TO/LOCAL/CSV.csv");
        RewardOrganizationImport.RewardOrganizationImportError expectedRowError = new RewardOrganizationImport.RewardOrganizationImportError(1, "ERRORCODE", "ERRORDESCRIPTION");

        Mockito.when(importsRepositoryMock.createIfNotExistsOrReturnEmpty(expectedImportRequest)).thenReturn(Mono.just(expectedImportRequest));
        Mockito.when(csvRetrieverServiceMock.retrieveCsv(expectedImportRequest)).thenAnswer(i->Mono.just(expectedLocalCsvPath));
        Mockito.when(importRewardNotificationFeedbackCsvServiceMock.evaluate(expectedLocalCsvPath, expectedImportRequest)).thenAnswer(i ->
                        Flux.just(new RewardsNotification())
                                        .doOnNext(r-> {
                                            expectedImportRequest.setRewardsResulted(1L);
                                            expectedImportRequest.setRewardsResultedError(1L);
                                            expectedImportRequest.setErrors(new ArrayList<>(List.of(expectedRowError)));
                                        })
        );
        Mockito.when(importsRepositoryMock.save(expectedImportRequest)).thenReturn(Mono.just(expectedImportRequest));

        // When
        List<RewardOrganizationImport> result = feedbackMediatorService.execute(List.of(event), MessageBuilder.withPayload("").build(), new HashMap<>()).block();

        // Then
        checkResult(result, expectedImportRequest, RewardOrganizationImportStatus.WARN, List.of(expectedRowError));
    }

    @Test
    void testErrorWhenElaborating(){
        // Given
        StorageEventDto event = StorageEventDtoFaker.mockInstance(0);
        RewardOrganizationImport expectedImportRequest = mapper.apply(event);
        Path expectedLocalCsvPath = Path.of("PATH/TO/LOCAL/CSV.csv");
        RewardOrganizationImport.RewardOrganizationImportError expectedErrorOnRow1 = new RewardOrganizationImport.RewardOrganizationImportError(1, "ERRORCODE", "ERRORDESC");

        AtomicInteger row = new AtomicInteger(0);
        Mockito.when(importsRepositoryMock.createIfNotExistsOrReturnEmpty(expectedImportRequest)).thenReturn(Mono.just(expectedImportRequest));
        Mockito.when(csvRetrieverServiceMock.retrieveCsv(expectedImportRequest)).thenAnswer(i->Mono.just(expectedLocalCsvPath));
        Mockito.when(importRewardNotificationFeedbackCsvServiceMock.evaluate(expectedLocalCsvPath, expectedImportRequest)).thenAnswer(a ->
                Flux.just(new RewardsNotification(), new RewardsNotification(), new RewardsNotification())
                        .doOnNext(r-> {
                            int i = row.getAndIncrement();
                            if(i==2){
                                throw new RuntimeException("DUMMY");
                            }
                            if(i==1){
                                expectedImportRequest.setRewardsResultedError(expectedImportRequest.getRewardsResultedError()+1);
                                expectedImportRequest.getErrors().add(expectedErrorOnRow1);
                            }
                            expectedImportRequest.setRewardsResulted(expectedImportRequest.getRewardsResulted()+1);
                        })
        );
        Mockito.when(importsRepositoryMock.save(expectedImportRequest)).thenReturn(Mono.just(expectedImportRequest));

        // When
        List<RewardOrganizationImport> result = feedbackMediatorService.execute(List.of(event), MessageBuilder.withPayload("").build(), new HashMap<>()).block();

        // Then
        checkResult(result, expectedImportRequest, RewardOrganizationImportStatus.ERROR,
                List.of(expectedErrorOnRow1, new RewardOrganizationImport.RewardOrganizationImportError(RewardFeedbackConstants.ImportFileErrors.GENERIC_ERROR)));
    }

    @Test
    void testSuccessful(){
        // Given
        StorageEventDto event = StorageEventDtoFaker.mockInstance(0);
        RewardOrganizationImport expectedImportRequest = mapper.apply(event);
        Path expectedLocalCsvPath = Path.of("PATH/TO/LOCAL/CSV.csv");

        Mockito.when(importsRepositoryMock.createIfNotExistsOrReturnEmpty(expectedImportRequest)).thenReturn(Mono.just(expectedImportRequest));
        Mockito.when(csvRetrieverServiceMock.retrieveCsv(expectedImportRequest)).thenAnswer(i->Mono.just(expectedLocalCsvPath));
        Mockito.when(importRewardNotificationFeedbackCsvServiceMock.evaluate(expectedLocalCsvPath, expectedImportRequest)).thenAnswer(i ->
                Flux.just(new RewardsNotification())
                        .doOnNext(r->expectedImportRequest.setRewardsResulted(1L))
        );
        Mockito.when(importsRepositoryMock.save(expectedImportRequest)).thenReturn(Mono.just(expectedImportRequest));

        // When
        List<RewardOrganizationImport> result = feedbackMediatorService.execute(List.of(event), MessageBuilder.withPayload("").build(), new HashMap<>()).block();

        // Then
        checkResult(result, expectedImportRequest, RewardOrganizationImportStatus.COMPLETE, Collections.emptyList());
    }

    private void checkResult(List<RewardOrganizationImport> result, RewardOrganizationImport expectedImportRequest, RewardOrganizationImportStatus expectedStatus, List<RewardOrganizationImport.RewardOrganizationImportError> expectedErrors) {
        Assertions.assertNotNull(result);
        Assertions.assertEquals(List.of(expectedImportRequest), result);

        Assertions.assertEquals(expectedErrors, expectedImportRequest.getErrors());
        Assertions.assertEquals(expectedImportRequest.getErrorsSize(), expectedImportRequest.getErrors().size());
        Assertions.assertEquals(expectedStatus, expectedImportRequest.getStatus());
    }
}
