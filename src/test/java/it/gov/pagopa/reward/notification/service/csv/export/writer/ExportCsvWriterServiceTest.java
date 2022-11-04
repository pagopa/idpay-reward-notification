package it.gov.pagopa.reward.notification.service.csv.export.writer;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import it.gov.pagopa.reward.notification.dto.mapper.RewardNotificationExport2CsvMapper;
import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationExportCsvDto;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.model.User;
import it.gov.pagopa.reward.notification.service.csv.export.retrieve.Iban2NotifyRetrieverService;
import it.gov.pagopa.reward.notification.service.csv.export.retrieve.User2NotifyRetrieverService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ExportCsvWriterServiceTest {

    @Mock private Iban2NotifyRetrieverService iban2NotifyRetrieverServiceMock;
    @Mock private User2NotifyRetrieverService user2NotifyRetrieverServiceMock;
    @Mock private RewardNotificationExport2CsvMapper rewardNotificationExport2CsvMapperMock;
    @Mock private StatefulBeanToCsv<RewardNotificationExportCsvDto> csvWriterMock;


    private ExportCsvWriterService service;

    @BeforeEach
    void init(){
        service = new ExportCsvWriterServiceImpl(iban2NotifyRetrieverServiceMock, user2NotifyRetrieverServiceMock, rewardNotificationExport2CsvMapperMock);
    }

    @Test
    void testNoIban(){
        testIbanNotSuccessful(Mono.empty());
    }
    @Test
    void testIbanException(){
        testIbanNotSuccessful(Mono.error(new RuntimeException("DUMMY")));
    }
    void testIbanNotSuccessful(Mono<RewardsNotification> ibanRetrieveResult){
        // Given
        RewardsNotification reward = new RewardsNotification();
        RewardOrganizationExport export = new RewardOrganizationExport();

        Mockito.when(iban2NotifyRetrieverServiceMock.retrieveIban(Mockito.same(reward))).thenReturn(ibanRetrieveResult);

        // When
        RewardsNotification result = service.writeLine(reward, export, csvWriterMock).block();

        // Then
        Assertions.assertNull(result);

        Mockito.verifyNoMoreInteractions(iban2NotifyRetrieverServiceMock, user2NotifyRetrieverServiceMock, rewardNotificationExport2CsvMapperMock, csvWriterMock);
    }

    @Test
    void testNoUser(){
        testUserNotSuccessful(Mono.empty());
    }
    @Test
    void testUserException(){
        testUserNotSuccessful(Mono.error(new RuntimeException("DUMMY")));
    }
    void testUserNotSuccessful(Mono<Pair<RewardsNotification, User>> userRetrieveResult){
        // Given
        RewardsNotification reward = new RewardsNotification();
        RewardOrganizationExport export = new RewardOrganizationExport();

        Mockito.when(iban2NotifyRetrieverServiceMock.retrieveIban(Mockito.same(reward))).thenReturn(Mono.just(reward));
        Mockito.when(user2NotifyRetrieverServiceMock.retrieveUser(Mockito.same(reward))).thenReturn(userRetrieveResult);

        // When
        RewardsNotification result = service.writeLine(reward, export, csvWriterMock).block();

        // Then
        Assertions.assertNull(result);

        Mockito.verifyNoMoreInteractions(iban2NotifyRetrieverServiceMock, user2NotifyRetrieverServiceMock, rewardNotificationExport2CsvMapperMock, csvWriterMock);
    }

    @Test
    void test() throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
        // Given
        RewardsNotification reward = new RewardsNotification();
        RewardOrganizationExport export = new RewardOrganizationExport();
        User user = new User();
        RewardNotificationExportCsvDto csvLine = new RewardNotificationExportCsvDto();

        Mockito.when(iban2NotifyRetrieverServiceMock.retrieveIban(Mockito.same(reward))).thenReturn(Mono.just(reward));
        Mockito.when(user2NotifyRetrieverServiceMock.retrieveUser(Mockito.same(reward))).thenReturn(Mono.just(Pair.of(reward, user)));
        Mockito.when(rewardNotificationExport2CsvMapperMock.apply(Mockito.same(reward), Mockito.same(user))).thenReturn(csvLine);

        // When
        RewardsNotification result = service.writeLine(reward, export, csvWriterMock).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertSame(reward, result);

        Mockito.verify(csvWriterMock).write(Mockito.same(csvLine));

        Mockito.verifyNoMoreInteractions(iban2NotifyRetrieverServiceMock, user2NotifyRetrieverServiceMock, rewardNotificationExport2CsvMapperMock, csvWriterMock);
    }

    @Test
    void testExceptionWhenWritingLine() throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
        // Given
        RewardsNotification reward = new RewardsNotification();
        RewardOrganizationExport export = new RewardOrganizationExport();
        User user = new User();
        RewardNotificationExportCsvDto csvLine = new RewardNotificationExportCsvDto();

        Mockito.when(iban2NotifyRetrieverServiceMock.retrieveIban(Mockito.same(reward))).thenReturn(Mono.just(reward));
        Mockito.when(user2NotifyRetrieverServiceMock.retrieveUser(Mockito.same(reward))).thenReturn(Mono.just(Pair.of(reward, user)));
        Mockito.when(rewardNotificationExport2CsvMapperMock.apply(Mockito.same(reward), Mockito.same(user))).thenReturn(csvLine);

        Mockito.doThrow(new CsvDataTypeMismatchException())
                .when(csvWriterMock)
                .write(Mockito.same(csvLine));

        // When
        RewardsNotification result = service.writeLine(reward, export, csvWriterMock).block();

        // Then
        Assertions.assertNull(result);

        Mockito.verify(csvWriterMock).write(Mockito.same(csvLine));

        Mockito.verifyNoMoreInteractions(iban2NotifyRetrieverServiceMock, user2NotifyRetrieverServiceMock, rewardNotificationExport2CsvMapperMock, csvWriterMock);
    }
}
