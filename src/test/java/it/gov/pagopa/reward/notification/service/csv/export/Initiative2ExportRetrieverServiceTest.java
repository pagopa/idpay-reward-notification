package it.gov.pagopa.reward.notification.service.csv.export;

import com.mongodb.DuplicateKeyException;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcernResult;
import it.gov.pagopa.reward.notification.enums.ExportStatus;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.repository.RewardNotificationRuleRepository;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationExportsRepository;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.utils.Utils;
import it.gov.pagopa.reward.notification.test.fakers.RewardNotificationRuleFaker;
import org.bson.BsonDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Example;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class Initiative2ExportRetrieverServiceTest {

    @Mock
    private RewardOrganizationExportsRepository rewardOrganizationExportRepositoryMock;
    @Mock
    private RewardsNotificationRepository rewardsNotificationRepositoryMock;
    @Mock
    private RewardNotificationRuleRepository rewardNotificationRuleRepositoryMock;

    private Initiative2ExportRetrieverServiceImpl service;

    private final String exportBasePath = "/export/base/path";
    private final LocalDate now = LocalDate.now();
    private final String nowFormatted = Utils.FORMATTER_DATE.format(now);

    @BeforeEach
    void init() {
        service = new Initiative2ExportRetrieverServiceImpl(exportBasePath, rewardOrganizationExportRepositoryMock, rewardsNotificationRepositoryMock, rewardNotificationRuleRepositoryMock);
    }

    @Test
    void testWhenReservationAlreadyExists() {
        RewardOrganizationExport expectedResult = new RewardOrganizationExport();
        Mockito.when(rewardOrganizationExportRepositoryMock.reserveExport()).thenReturn(Mono.just(expectedResult));
        Mockito.when(rewardOrganizationExportRepositoryMock.findPendingAndTodayExports()).thenReturn(Flux.empty());

        RewardOrganizationExport result = service.retrieve().block();
        Assertions.assertSame(expectedResult, result);

        Mockito.verifyNoMoreInteractions(rewardOrganizationExportRepositoryMock, rewardsNotificationRepositoryMock, rewardNotificationRuleRepositoryMock);
    }

    @Test
    void testWhenReservationNotExistsAndNotPendingExportsAndNotRewards2Notify() {
        testWhenReservationNotExistsAndPendingExportsAndNotRewards2Notify(Collections.emptyList());
    }

    @Test
    void testWhenReservationNotExistsAndNotRewards2Notify() {
        testWhenReservationNotExistsAndPendingExportsAndNotRewards2Notify(List.of(
                RewardOrganizationExport.builder().initiativeId("INITIATIVE_PENDING_1").build(),
                RewardOrganizationExport.builder().initiativeId("INITIATIVE_PENDING_2").build()
        ));
    }
    void testWhenReservationNotExistsAndPendingExportsAndNotRewards2Notify(List<RewardOrganizationExport> expectedPendingExports) {
        Mockito.when(rewardOrganizationExportRepositoryMock.reserveExport()).thenReturn(Mono.empty());
        Mockito.when(rewardOrganizationExportRepositoryMock.findPendingAndTodayExports()).thenReturn(Flux.fromIterable(expectedPendingExports));
        Mockito.when(rewardsNotificationRepositoryMock.findInitiatives2Notify(
                expectedPendingExports.stream().map(RewardOrganizationExport::getInitiativeId).toList()))
                .thenReturn(Flux.empty());

        RewardOrganizationExport result = service.retrieve().block();
        Assertions.assertNull(result);

        Mockito.verifyNoMoreInteractions(rewardOrganizationExportRepositoryMock, rewardsNotificationRepositoryMock, rewardNotificationRuleRepositoryMock);
    }

    @Test
    void testWhenReservationNotExistsAndInitiativeNotExists() {
        Mockito.when(rewardOrganizationExportRepositoryMock.reserveExport()).thenReturn(Mono.empty());
        Mockito.when(rewardOrganizationExportRepositoryMock.findPendingAndTodayExports()).thenReturn(Flux.empty());
        Mockito.when(rewardsNotificationRepositoryMock.findInitiatives2Notify(Collections.emptyList())).thenReturn(Flux.just("INITIATIVEID1"));

        Mockito.when(rewardNotificationRuleRepositoryMock.findById("INITIATIVEID1")).thenReturn(Mono.empty());

        RewardOrganizationExport result = service.retrieve().block();
        Assertions.assertNull(result);

        Mockito.verifyNoMoreInteractions(rewardOrganizationExportRepositoryMock, rewardsNotificationRepositoryMock, rewardNotificationRuleRepositoryMock);
    }

    @Test
    void testWhenReservationNotExistsAndNotPendingExports() {
        testWhenReservationNotExistsAndPendingExports(Collections.emptyList());
    }
    @Test
    void testWhenReservationNotExists() {
        testWhenReservationNotExistsAndPendingExports(List.of(
                RewardOrganizationExport.builder().initiativeId("INITIATIVE_PENDING_1").build(),
                RewardOrganizationExport.builder().initiativeId("INITIATIVE_PENDING_2").build()
        ));
    }
    void testWhenReservationNotExistsAndPendingExports(List<RewardOrganizationExport> expectedPendingExports) {
        Mockito.when(rewardOrganizationExportRepositoryMock.findPendingAndTodayExports()).thenReturn(Flux.fromIterable(expectedPendingExports));
        Mockito.when(rewardsNotificationRepositoryMock.findInitiatives2Notify(
                expectedPendingExports.stream().map(RewardOrganizationExport::getInitiativeId).toList()))
                .thenReturn(Flux.just("INITIATIVEID1", "INITIATIVEID2", "INITIATIVE_ALREADY_RESERVED", "INITIATIVE_EXPORT_JUST_STORED"));

        RewardNotificationRule rule1 = RewardNotificationRuleFaker.mockInstance(1);
        RewardNotificationRule rule2 = RewardNotificationRuleFaker.mockInstance(2);
        RewardNotificationRule rule3_alreadyReserved = RewardNotificationRuleFaker.mockInstance(3);
        RewardNotificationRule rule4_alreadyStored = RewardNotificationRuleFaker.mockInstance(4);

        Mockito.when(rewardNotificationRuleRepositoryMock.findById("INITIATIVEID1")).thenReturn(Mono.just(rule1));
        Mockito.when(rewardNotificationRuleRepositoryMock.findById("INITIATIVEID2")).thenReturn(Mono.just(rule2));
        Mockito.when(rewardNotificationRuleRepositoryMock.findById("INITIATIVE_ALREADY_RESERVED")).thenReturn(Mono.just(rule3_alreadyReserved));
        Mockito.when(rewardNotificationRuleRepositoryMock.findById("INITIATIVE_EXPORT_JUST_STORED")).thenReturn(Mono.just(rule4_alreadyStored));

        Mockito.when(rewardOrganizationExportRepositoryMock.count(Example.of(
                        RewardOrganizationExport.builder()
                                .initiativeId("INITIATIVEID1")
                                .notificationDate(now)
                                .build())))
                .thenReturn(Mono.empty());
        Mockito.when(rewardOrganizationExportRepositoryMock.count(Example.of(
                        RewardOrganizationExport.builder()
                                .initiativeId("INITIATIVEID2")
                                .notificationDate(now)
                                .build())))
                .thenReturn(Mono.just(2L));
        Mockito.when(rewardOrganizationExportRepositoryMock.count(Example.of(
                        RewardOrganizationExport.builder()
                                .initiativeId("INITIATIVE_ALREADY_RESERVED")
                                .notificationDate(now)
                                .build())))
                .thenReturn(Mono.just(0L));
        Mockito.when(rewardOrganizationExportRepositoryMock.count(Example.of(
                        RewardOrganizationExport.builder()
                                .initiativeId("INITIATIVE_EXPORT_JUST_STORED")
                                .notificationDate(now)
                                .build())))
                .thenReturn(Mono.just(0L));

        RewardOrganizationExport expectedNewRewardNotification1 = buildNewExpectedRewardNotification(rule1, 1L);
        RewardOrganizationExport expectedNewRewardNotification2 = buildNewExpectedRewardNotification(rule2, 3L);
        RewardOrganizationExport expectedNewRewardNotification3 = buildNewExpectedRewardNotification(rule3_alreadyReserved, 1L);
        RewardOrganizationExport expectedNewRewardNotification4 = buildNewExpectedRewardNotification(rule4_alreadyStored, 1L);

        Mockito.when(rewardOrganizationExportRepositoryMock.configureNewExport(expectedNewRewardNotification1)).thenReturn(Mono.just(expectedNewRewardNotification1));
        Mockito.when(rewardOrganizationExportRepositoryMock.configureNewExport(expectedNewRewardNotification2)).thenReturn(Mono.just(expectedNewRewardNotification2));
        Mockito.when(rewardOrganizationExportRepositoryMock.configureNewExport(expectedNewRewardNotification3)).thenReturn(Mono.empty());
        Mockito.when(rewardOrganizationExportRepositoryMock.configureNewExport(expectedNewRewardNotification4)).thenReturn(Mono.error(new DuplicateKeyException(new BsonDocument(), new ServerAddress(), WriteConcernResult.unacknowledged())));

        int[] invocationNumber = new int[]{0};
        Mockito.doAnswer(a -> {
                    if (invocationNumber[0]++ == 0) {
                        return Mono.empty();
                    } else {
                        return Mono.just(expectedNewRewardNotification1);
                    }
                }).
                when(rewardOrganizationExportRepositoryMock).reserveExport();

        RewardOrganizationExport result = service.retrieve().block();
        Assertions.assertSame(expectedNewRewardNotification1, result);

        Mockito.verifyNoMoreInteractions(rewardOrganizationExportRepositoryMock, rewardsNotificationRepositoryMock, rewardNotificationRuleRepositoryMock);
    }

    private RewardOrganizationExport buildNewExpectedRewardNotification(RewardNotificationRule rule, long progressive) {
        return RewardOrganizationExport.builder()
                .id("%s_%s.%d".formatted(rule.getInitiativeId(), nowFormatted, progressive))
                .filePath("%s/%s/%s/%s".formatted(
                        exportBasePath,
                        rule.getOrganizationId(),
                        rule.getInitiativeId(),
                        rule.getInitiativeName() + "_" + nowFormatted + "." + progressive + ".zip"
                ))
                .initiativeId(rule.getInitiativeId())
                .initiativeName(rule.getInitiativeName())
                .organizationId(rule.getOrganizationId())
                .notificationDate(now)
                .status(ExportStatus.TODO)

                .rewardsExportedCents(0L)
                .rewardsResultsCents(0L)

                .rewardNotified(0L)
                .rewardsResulted(0L)
                .rewardsResultedOk(0L)

                .percentageResults(0L)
                .percentageResulted(0L)
                .percentageResultedOk(0L)

                .build();
    }
}