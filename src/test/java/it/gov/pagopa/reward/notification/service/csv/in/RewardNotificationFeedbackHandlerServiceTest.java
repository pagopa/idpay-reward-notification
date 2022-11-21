package it.gov.pagopa.reward.notification.service.csv.in;

import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationImportCsvDto;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationImportResult;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.service.csv.in.retrieve.RewardNotificationExportFeedbackRetrieverService;
import it.gov.pagopa.reward.notification.service.csv.in.retrieve.RewardNotificationFeedbackRetrieverService;
import it.gov.pagopa.reward.notification.service.csv.in.utils.FeedbackEvaluationException;
import it.gov.pagopa.reward.notification.service.csv.in.utils.RewardNotificationFeedbackHandlerOutcome;
import it.gov.pagopa.reward.notification.test.fakers.RewardOrganizationExportsFaker;
import it.gov.pagopa.reward.notification.test.fakers.RewardsNotificationFaker;
import it.gov.pagopa.reward.notification.utils.RewardFeedbackConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.HashMap;

@ExtendWith(MockitoExtension.class)
class RewardNotificationFeedbackHandlerServiceTest {

    @Mock private RewardNotificationFeedbackRetrieverService notificationFeedbackRetrieverServiceMock;
    @Mock private RewardNotificationExportFeedbackRetrieverService exportFeedbackRetrieverServiceMock;

    private RewardNotificationFeedbackHandlerService service;

    @BeforeEach
    void init(){
        service = new RewardNotificationFeedbackHandlerServiceImpl(notificationFeedbackRetrieverServiceMock, exportFeedbackRetrieverServiceMock);
    }

    @AfterEach
    void checkMocks(){
        Mockito.verifyNoMoreInteractions(notificationFeedbackRetrieverServiceMock, exportFeedbackRetrieverServiceMock);
    }

    @Test
    void testInvalidResult(){
        // Given
        RewardOrganizationImport importRequest = new RewardOrganizationImport();
        HashMap<String, RewardOrganizationExport> exportCache = new HashMap<>();
        RewardNotificationImportCsvDto row = new RewardNotificationImportCsvDto();
        row.setRowNumber(5);

        RewardNotificationFeedbackHandlerOutcome expectedResult = new RewardNotificationFeedbackHandlerOutcome(null, null, new RewardOrganizationImport.RewardOrganizationImportError(5, RewardFeedbackConstants.ImportFeedbackRowErrors.INVALID_RESULT));

        // When
        RewardNotificationFeedbackHandlerOutcome result = service.evaluate(row, importRequest, exportCache).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    void testGenericError(){
        // Given
        RewardOrganizationImport importRequest = new RewardOrganizationImport();
        HashMap<String, RewardOrganizationExport> exportCache = new HashMap<>();
        RewardNotificationImportCsvDto row = new RewardNotificationImportCsvDto();
        row.setRowNumber(5);
        row.setResult(RewardOrganizationImportResult.OK.value);

        Mockito.when(notificationFeedbackRetrieverServiceMock.retrieve(Mockito.same(row), Mockito.same(importRequest))).thenReturn(Mono.error(new RuntimeException("DUMMY")));

        RewardNotificationFeedbackHandlerOutcome expectedResult = new RewardNotificationFeedbackHandlerOutcome(RewardOrganizationImportResult.OK, null, new RewardOrganizationImport.RewardOrganizationImportError(5, RewardFeedbackConstants.ImportFeedbackRowErrors.GENERIC_ERROR));

        // When
        RewardNotificationFeedbackHandlerOutcome result = service.evaluate(row, importRequest, exportCache).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    void testFeedbackError(){
        // Given
        RewardOrganizationImport importRequest = new RewardOrganizationImport();
        HashMap<String, RewardOrganizationExport> exportCache = new HashMap<>();
        RewardNotificationImportCsvDto row = new RewardNotificationImportCsvDto();
        row.setRowNumber(5);
        row.setResult(RewardOrganizationImportResult.KO.value);

        Mockito.when(notificationFeedbackRetrieverServiceMock.retrieve(Mockito.same(row), Mockito.same(importRequest))).thenReturn(Mono.error(new FeedbackEvaluationException(RewardFeedbackConstants.ImportFeedbackRowErrors.NOT_FOUND)));

        RewardNotificationFeedbackHandlerOutcome expectedResult = new RewardNotificationFeedbackHandlerOutcome(RewardOrganizationImportResult.KO, null, new RewardOrganizationImport.RewardOrganizationImportError(5, RewardFeedbackConstants.ImportFeedbackRowErrors.NOT_FOUND));

        // When
        RewardNotificationFeedbackHandlerOutcome result = service.evaluate(row, importRequest, exportCache).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    void testNotToNotify(){
        // Given
        RewardOrganizationImport importRequest = new RewardOrganizationImport();
        HashMap<String, RewardOrganizationExport> exportCache = new HashMap<>();
        RewardNotificationImportCsvDto row = new RewardNotificationImportCsvDto();
        row.setRowNumber(5);
        row.setResult(RewardOrganizationImportResult.OK.value);

        RewardsNotification notification = RewardsNotificationFaker.mockInstance(0);
        RewardOrganizationExport export = RewardOrganizationExportsFaker.mockInstance(0);
        notification.setExportId(export.getId());

        Mockito.when(notificationFeedbackRetrieverServiceMock.retrieve(Mockito.same(row), Mockito.same(importRequest))).thenReturn(Mono.just(notification));
        Mockito.when(exportFeedbackRetrieverServiceMock.retrieve(Mockito.same(notification), Mockito.same(row), Mockito.same(importRequest), Mockito.same(exportCache))).thenReturn(Mono.just(export));

        Mockito.when(notificationFeedbackRetrieverServiceMock.updateFeedbackHistory(Mockito.same(notification), Mockito.same(row), Mockito.eq(RewardOrganizationImportResult.OK), Mockito.same(importRequest))).thenReturn(Mono.just(false));

        RewardNotificationFeedbackHandlerOutcome expectedResult = new RewardNotificationFeedbackHandlerOutcome(RewardOrganizationImportResult.OK, export.getId(), null);

        // When
        RewardNotificationFeedbackHandlerOutcome result = service.evaluate(row, importRequest, exportCache).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    void testToNotify(){
        // TODO
    }
}
