package it.gov.pagopa.reward.notification.service.csv.in.retrieve;

import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationImportCsvDto;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationImportResult;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.csv.in.utils.FeedbackEvaluationException;
import it.gov.pagopa.reward.notification.test.fakers.RewardOrganizationImportFaker;
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

import java.time.LocalDate;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class RewardNotificationFeedbackRetrieverServiceTest {

    @Mock private RewardsNotificationRepository repositoryMock;

    private RewardNotificationFeedbackRetrieverService service;

    @BeforeEach
    void init(){
        service = new RewardNotificationFeedbackRetrieverServiceImpl(repositoryMock);
    }

    @AfterEach
    void checkMocks(){
        Mockito.verifyNoMoreInteractions(repositoryMock);
    }

    // region test retrieve

    @Test
    void testRetrieve_NotFound(){
        // Given
        RewardOrganizationImport importRequest = new RewardOrganizationImport();
        importRequest.setInitiativeId("INITITATIVEID");
        importRequest.setOrganizationId("ORGANIZATIONID");

        RewardNotificationImportCsvDto row = new RewardNotificationImportCsvDto();
        row.setUniqueID("EXTERNALID");

        Mockito.when(repositoryMock.findByExternalId(row.getUniqueID())).thenReturn(Mono.empty());

        // When
        Mono<RewardsNotification> mono = service.retrieve(row, importRequest);

        // Then
        try {
            mono.block();
            Assertions.fail("Exception expected");
        } catch (FeedbackEvaluationException e){
            Assertions.assertEquals(RewardFeedbackConstants.ImportFeedbackRowErrors.NOT_FOUND, e.getError());
        }
    }

    @Test
    void testRetrieve_UnexpectedInitiativeId(){
        // Given
        RewardOrganizationImport importRequest = new RewardOrganizationImport();
        importRequest.setInitiativeId("INITITATIVEID");
        importRequest.setOrganizationId("ORGANIZATIONID");

        RewardNotificationImportCsvDto row = new RewardNotificationImportCsvDto();
        row.setUniqueID("EXTERNALID");

        RewardsNotification rn = new RewardsNotification();
        rn.setInitiativeId("UNEXPECTEDINITITATIVEID");
        rn.setOrganizationId("ORGANIZATIONID");

        Mockito.when(repositoryMock.findByExternalId(row.getUniqueID())).thenReturn(Mono.just(rn));

        // When
        Mono<RewardsNotification> mono = service.retrieve(row, importRequest);

        // Then
        try {
            mono.block();
            Assertions.fail("Exception expected");
        } catch (FeedbackEvaluationException e){
            Assertions.assertEquals(RewardFeedbackConstants.ImportFeedbackRowErrors.NOT_FOUND, e.getError());
        }
    }

    @Test
    void testRetrieve_UnexpectedOrganizationId(){
        // Given
        RewardOrganizationImport importRequest = new RewardOrganizationImport();
        importRequest.setInitiativeId("INITIATIVEID");
        importRequest.setOrganizationId("ORGANIZATIONID");

        RewardNotificationImportCsvDto row = new RewardNotificationImportCsvDto();
        row.setUniqueID("EXTERNALID");

        RewardsNotification rn = new RewardsNotification();
        rn.setInitiativeId("INITITATIVEID");
        rn.setOrganizationId("UNEXPECTEDORGANIZATIONID");

        Mockito.when(repositoryMock.findByExternalId(row.getUniqueID())).thenReturn(Mono.just(rn));

        // When
        Mono<RewardsNotification> mono = service.retrieve(row, importRequest);

        // Then
        try {
            mono.block();
            Assertions.fail("Exception expected");
        } catch (FeedbackEvaluationException e){
            Assertions.assertEquals(RewardFeedbackConstants.ImportFeedbackRowErrors.NOT_FOUND, e.getError());
        }
    }

    @Test
    void testRetrieve_Ok(){
        // Given
        RewardOrganizationImport importRequest = new RewardOrganizationImport();
        importRequest.setInitiativeId("INITIATIVEID");
        importRequest.setOrganizationId("ORGANIZATIONID");

        RewardNotificationImportCsvDto row = new RewardNotificationImportCsvDto();
        row.setUniqueID("EXTERNALID");

        RewardsNotification rn = new RewardsNotification();
        rn.setInitiativeId("INITIATIVEID");
        rn.setOrganizationId("ORGANIZATIONID");

        Mockito.when(repositoryMock.findByExternalId(row.getUniqueID())).thenReturn(Mono.just(rn));

        // When
        RewardsNotification result = service.retrieve(row, importRequest).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertSame(rn, result);
    }
    //endregion

    // region updateFeedbackHistory
    @Test
    void testUpdateFeedbackHistory_AlreadySeen(){
        // Given
        RewardOrganizationImport importRequest = RewardOrganizationImportFaker.mockInstance(0);
        RewardsNotification notification = RewardsNotificationFaker.mockInstance(0);
        RewardNotificationImportCsvDto row = new RewardNotificationImportCsvDto();

        notification.getFeedbackHistory().add(RewardsNotification.RewardNotificationHistory.fromImportRow(row, RewardOrganizationImportResult.OK, importRequest));

        // When
        Boolean result = service.updateFeedbackHistory(notification, row, RewardOrganizationImportResult.OK, importRequest).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result);
    }

    @Test
    void testUpdateFeedbackHistory_PastFeedback(){
        // Given
        RewardOrganizationImport importRequest = RewardOrganizationImportFaker.mockInstance(0);
        RewardsNotification notification = RewardsNotificationFaker.mockInstance(0);

        RewardNotificationImportCsvDto row = new RewardNotificationImportCsvDto();

        RewardsNotification.RewardNotificationHistory feedbackStored = RewardsNotification.RewardNotificationHistory.fromImportRow(row, RewardOrganizationImportResult.OK, importRequest);
        feedbackStored.setFeedbackFilePath("OTHER/FILE");
        feedbackStored.setFeedbackDate(importRequest.getFeedbackDate().plusDays(1));

        notification.getFeedbackHistory().add(feedbackStored);
        notification.setFeedbackDate(feedbackStored.getFeedbackDate());
        notification.setExecutionDate(LocalDate.now());
        notification.setCro("PREVIOUSCRO");
        notification.setStatus(RewardNotificationStatus.COMPLETED_OK);
        notification.setResultCode(RewardOrganizationImportResult.OK.value);
        notification.setRejectionReason("PREVIOUS");

        Mockito.when(repositoryMock.save(Mockito.same(notification))).thenReturn(Mono.just(notification));

        // When
        Boolean result = service.updateFeedbackHistory(notification, row, RewardOrganizationImportResult.OK, importRequest).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result);

        Assertions.assertEquals(List.of(
                        RewardsNotification.RewardNotificationHistory.fromImportRow(row, RewardOrganizationImportResult.OK, importRequest),
                        feedbackStored
                ),
                notification.getFeedbackHistory());

        Assertions.assertEquals(feedbackStored.getFeedbackDate(), notification.getFeedbackDate());
        Assertions.assertEquals(RewardNotificationStatus.COMPLETED_OK, notification.getStatus());
        Assertions.assertEquals(LocalDate.now(), notification.getExecutionDate());
        Assertions.assertEquals("PREVIOUSCRO", notification.getCro());
        Assertions.assertEquals(RewardOrganizationImportResult.OK.value, notification.getResultCode());
        Assertions.assertEquals("PREVIOUS", notification.getRejectionReason());
    }

    @Test
    void testUpdateFeedbackHistory_NewFeedback(){
        // Given
        RewardOrganizationImport importRequest = RewardOrganizationImportFaker.mockInstance(0);
        RewardsNotification notification = RewardsNotificationFaker.mockInstance(0);
        RewardNotificationImportCsvDto row = new RewardNotificationImportCsvDto();
        row.setResult("NEWRESULTCODE");
        row.setRejectionReason("NEWREJECTIONREASON");
        row.setExecutionDate(LocalDate.now().plusDays(2));
        row.setCro("CRO");

        RewardsNotification.RewardNotificationHistory feedbackStored = RewardsNotification.RewardNotificationHistory.fromImportRow(row, RewardOrganizationImportResult.OK, importRequest);
        feedbackStored.setFeedbackFilePath("OTHER/FILE");
        feedbackStored.setFeedbackDate(importRequest.getFeedbackDate().minusDays(1));

        notification.getFeedbackHistory().add(feedbackStored);
        notification.setFeedbackDate(feedbackStored.getFeedbackDate());
        notification.setExecutionDate(LocalDate.now());
        notification.setCro("PREVIOUSCRO");
        notification.setStatus(RewardNotificationStatus.COMPLETED_KO);
        notification.setResultCode(RewardOrganizationImportResult.KO.value);
        notification.setRejectionReason("PREVIOUS_KO");

        Mockito.when(repositoryMock.save(Mockito.same(notification))).thenReturn(Mono.just(notification));

        // When
        Boolean result = service.updateFeedbackHistory(notification, row, RewardOrganizationImportResult.OK, importRequest).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result);

        Assertions.assertEquals(List.of(
                        feedbackStored,
                        RewardsNotification.RewardNotificationHistory.fromImportRow(row, RewardOrganizationImportResult.OK, importRequest)
                ),
                notification.getFeedbackHistory());

        Assertions.assertEquals(importRequest.getFeedbackDate(), notification.getFeedbackDate());
        Assertions.assertEquals(RewardNotificationStatus.COMPLETED_OK, notification.getStatus());
        Assertions.assertEquals(LocalDate.now().plusDays(2), notification.getExecutionDate());
        Assertions.assertEquals("CRO", notification.getCro());
        Assertions.assertEquals("NEWRESULTCODE", notification.getResultCode());
        Assertions.assertEquals("NEWREJECTIONREASON", notification.getRejectionReason());
    }

    @Test
    void testUpdateFeedbackHistory_NoFeedback(){
        // Given
        RewardOrganizationImport importRequest = RewardOrganizationImportFaker.mockInstance(0);
        RewardsNotification notification = RewardsNotificationFaker.mockInstance(0);
        RewardNotificationImportCsvDto row = new RewardNotificationImportCsvDto();
        row.setResult("RESULTCODE");

        Mockito.when(repositoryMock.save(Mockito.same(notification))).thenReturn(Mono.just(notification));

        // When
        Boolean result = service.updateFeedbackHistory(notification, row, RewardOrganizationImportResult.OK, importRequest).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result);

        Assertions.assertEquals(List.of(
                        RewardsNotification.RewardNotificationHistory.fromImportRow(row, RewardOrganizationImportResult.OK, importRequest)
                ),
                notification.getFeedbackHistory());

        Assertions.assertEquals(importRequest.getFeedbackDate(), notification.getFeedbackDate());
        Assertions.assertEquals(RewardNotificationStatus.COMPLETED_OK, notification.getStatus());
        Assertions.assertEquals("RESULTCODE", notification.getResultCode());
        Assertions.assertNull(notification.getRejectionReason());
    }
    //endregion
}
