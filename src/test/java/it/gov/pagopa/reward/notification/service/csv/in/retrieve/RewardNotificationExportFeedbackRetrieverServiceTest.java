package it.gov.pagopa.reward.notification.service.csv.in.retrieve;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationImportCsvDto;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationExportStatus;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationExportsRepository;
import it.gov.pagopa.reward.notification.test.fakers.RewardOrganizationExportsFaker;
import it.gov.pagopa.reward.notification.test.fakers.RewardsNotificationFaker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ExtendWith(MockitoExtension.class)
class RewardNotificationExportFeedbackRetrieverServiceTest {

    @Mock private RewardOrganizationExportsRepository repositoryMock;

    private RewardNotificationExportFeedbackRetrieverService service;

    @BeforeEach
    void init(){
        service = new RewardNotificationExportFeedbackRetrieverServiceImpl(repositoryMock);
    }

    @AfterEach
    void checkMocks(){
        Mockito.verifyNoMoreInteractions(repositoryMock);
    }

    //region test retrieve
    @Test
    void testRetrieve_NotFound(){
        // Given
        RewardOrganizationImport importRequest = new RewardOrganizationImport();
        importRequest.setInitiativeId("INITITATIVEID");
        importRequest.setOrganizationId("ORGANIZATIONID");

        RewardNotificationImportCsvDto row = new RewardNotificationImportCsvDto();
        row.setUniqueID("EXTERNALID");

        RewardsNotification rn = RewardsNotificationFaker.mockInstance(0);
        rn.setExportId("EXPORTID");
        rn.setInitiativeId("INITITATIVEID");
        rn.setOrganizationId("ORGANIZATIONID");
        rn.setExportDate(LocalDateTime.now());

        Mockito.when(repositoryMock.findById(rn.getExportId())).thenReturn(Mono.empty());

        Mockito.when(repositoryMock.save(Mockito.any())).thenAnswer(a->Mono.just(a.getArgument(0)));

        Map<String, RewardOrganizationExport> exportCache = new ConcurrentHashMap<>();

        // When
        RewardOrganizationExport result = service.retrieve(rn, row, importRequest, exportCache)
                .flatMap(x -> service.retrieve(rn, row, importRequest, exportCache)) // called again to test cache behavior
                .block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(
                RewardOrganizationExport.builder()
                        .id(rn.getExportId())
                        .initiativeId(importRequest.getInitiativeId())
                        .organizationId(importRequest.getOrganizationId())
                        .notificationDate(rn.getNotificationDate())
                        .exportDate(rn.getExportDate().toLocalDate())
                        .rewardsExportedCents(-1L)
                        .rewardNotified(-1L)
                        .status(RewardOrganizationExportStatus.EXPORTED)
                        .build(),
                result
        );

        Assertions.assertEquals(1, exportCache.size());
        Assertions.assertSame(result, exportCache.get(rn.getExportId()));

        Mockito.verify(repositoryMock).findById(Mockito.anyString());
    }

    @Test
    void testRetrieve_Success(){
        // Given
        RewardOrganizationImport importRequest = new RewardOrganizationImport();
        importRequest.setInitiativeId("INITITATIVEID");
        importRequest.setOrganizationId("ORGANIZATIONID");

        RewardNotificationImportCsvDto row = new RewardNotificationImportCsvDto();
        row.setUniqueID("EXTERNALID");

        RewardsNotification rn = RewardsNotificationFaker.mockInstance(0);
        rn.setExportId("EXPORTID");
        rn.setInitiativeId("INITITATIVEID");
        rn.setOrganizationId("ORGANIZATIONID");
        rn.setExportDate(LocalDateTime.now());

        RewardOrganizationExport expectedExport = RewardOrganizationExportsFaker.mockInstance(0);

        Mockito.when(repositoryMock.findById(rn.getExportId())).thenReturn(Mono.just(expectedExport));

        Map<String, RewardOrganizationExport> exportCache = new ConcurrentHashMap<>();

        // When
        RewardOrganizationExport result = service.retrieve(rn, row, importRequest, exportCache)
                .flatMap(x -> service.retrieve(rn, row, importRequest, exportCache)) // called again to test cache behavior
                .block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertSame(expectedExport, result);

        Assertions.assertEquals(1, exportCache.size());
        Assertions.assertSame(result, exportCache.get(rn.getExportId()));

        Mockito.verify(repositoryMock).findById(Mockito.anyString());
    }
    //endregion

    @Test
    void testUpdateCounters(){
        // Given
        BigDecimal incReward = BigDecimal.ONE;
        long incCount = 5L;
        long incOkCount = 7L;
        RewardOrganizationExport export = new RewardOrganizationExport();

        UpdateResult expectedResult = Mockito.mock(UpdateResult.class);

        Mockito.when(repositoryMock.updateCounters(Mockito.same(incCount), Mockito.same(incReward), Mockito.same(incOkCount), Mockito.same(export))).thenReturn(Mono.just(expectedResult));

        // When
        UpdateResult result = service.updateCounters(incCount, incReward, incOkCount, export).block();

        // Then
        Assertions.assertSame(expectedResult, result);
    }
}
