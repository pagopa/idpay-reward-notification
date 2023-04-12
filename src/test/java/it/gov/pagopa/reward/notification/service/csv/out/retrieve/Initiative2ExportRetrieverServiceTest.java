package it.gov.pagopa.reward.notification.service.csv.out.retrieve;

import com.mongodb.DuplicateKeyException;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcernResult;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationExportStatus;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.repository.RewardNotificationRuleRepository;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationExportsRepository;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.utils.Utils;
import it.gov.pagopa.reward.notification.test.fakers.RewardNotificationRuleFaker;
import it.gov.pagopa.reward.notification.test.utils.TestUtils;
import org.bson.BsonDocument;
import org.junit.jupiter.api.AfterEach;
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
import java.util.stream.Collectors;

@ExtendWith(MockitoExtension.class)
class Initiative2ExportRetrieverServiceTest {

    @Mock
    private RewardOrganizationExportsRepository rewardOrganizationExportRepositoryMock;
    @Mock
    private RewardsNotificationRepository rewardsNotificationRepositoryMock;
    @Mock
    private RewardNotificationRuleRepository rewardNotificationRuleRepositoryMock;

    private Initiative2ExportRetrieverServiceImpl service;

    private final LocalDate now = LocalDate.now();
    private final String nowFormatted = Utils.FORMATTER_DATE.format(now);

    @BeforeEach
    void init() {
        service = new Initiative2ExportRetrieverServiceImpl("", rewardOrganizationExportRepositoryMock, rewardsNotificationRepositoryMock, rewardNotificationRuleRepositoryMock);
    }

    @AfterEach
    void checkNotMoreInteraction() {
        Mockito.verifyNoMoreInteractions(rewardOrganizationExportRepositoryMock, rewardsNotificationRepositoryMock, rewardNotificationRuleRepositoryMock);
    }

    //region retrieveStuckExecution
    @Test
    void testWhenNotStuckReservation() {
        Mockito.when(rewardOrganizationExportRepositoryMock.reserveStuckExport()).thenReturn(Mono.empty());

        RewardOrganizationExport result = service.retrieveStuckExecution().block();
        Assertions.assertNull(result);
    }

    @Test
    void testWhenStuckReservation() {
        RewardOrganizationExport expected = new RewardOrganizationExport();
        Mockito.when(rewardOrganizationExportRepositoryMock.reserveStuckExport()).thenReturn(Mono.just(expected));

        RewardOrganizationExport result = service.retrieveStuckExecution().block();
        Assertions.assertSame(expected, result);
    }
//endregion

    //region reserveExport
    @Test
    void testWhenReservationAlreadyExists() {
        RewardOrganizationExport expectedResult = new RewardOrganizationExport();
        Mockito.when(rewardOrganizationExportRepositoryMock.reserveExport()).thenReturn(Mono.just(expectedResult));
        Mockito.when(rewardOrganizationExportRepositoryMock.findPendingOrTodayExports()).thenReturn(Flux.just(expectedResult));

        RewardOrganizationExport result = service.retrieve(now).block();
        Assertions.assertSame(expectedResult, result);
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
        Mockito.when(rewardOrganizationExportRepositoryMock.findPendingOrTodayExports()).thenReturn(Flux.fromIterable(expectedPendingExports));
        Mockito.when(rewardsNotificationRepositoryMock.findInitiatives2Notify(
                        expectedPendingExports.stream().map(RewardOrganizationExport::getInitiativeId).collect(Collectors.toSet()),
                        now))
                .thenReturn(Flux.empty());

        RewardOrganizationExport result = service.retrieve(now).block();
        Assertions.assertNull(result);
    }

    @Test
    void testWhenReservationNotExistsAndInitiativeNotExists() {

        Mockito.when(rewardOrganizationExportRepositoryMock.reserveExport()).thenReturn(Mono.empty());
        Mockito.when(rewardOrganizationExportRepositoryMock.findPendingOrTodayExports()).thenReturn(Flux.empty());
        Mockito.when(rewardsNotificationRepositoryMock.findInitiatives2Notify(Collections.emptySet(), now)).thenReturn(Flux.just("INITIATIVEID1"));

        Mockito.when(rewardNotificationRuleRepositoryMock.findById("INITIATIVEID1")).thenReturn(Mono.empty());

        RewardOrganizationExport result = service.retrieve(now).block();
        Assertions.assertNull(result);
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
        Mockito.when(rewardOrganizationExportRepositoryMock.findPendingOrTodayExports()).thenReturn(Flux.fromIterable(expectedPendingExports));
        Mockito.when(rewardsNotificationRepositoryMock.findInitiatives2Notify(
                        expectedPendingExports.stream().map(RewardOrganizationExport::getInitiativeId).collect(Collectors.toSet()),
                        now))
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

        RewardOrganizationExport result = service.retrieve(now).block();
        Assertions.assertSame(expectedNewRewardNotification1, result);

        Mockito.verify(rewardOrganizationExportRepositoryMock, Mockito.never()).count(Mockito.argThat(i -> {
            TestUtils.checkNullFields(i.getProbe(), "initiativeId", "notificationDate");
            return false;
        }));
    }

    private RewardOrganizationExport buildNewExpectedRewardNotification(RewardNotificationRule rule, long progressive) {
        return RewardOrganizationExport.builder()
                .id("%s_%s.%d".formatted(rule.getInitiativeId(), nowFormatted, progressive))
                .filePath("%s/%s/export/%s".formatted(
                        rule.getOrganizationId(),
                        rule.getInitiativeId(),
                        rule.getInitiativeName() + "_" + nowFormatted + "." + progressive + ".zip"
                ))
                .initiativeId(rule.getInitiativeId())
                .initiativeName(rule.getInitiativeName())
                .organizationId(rule.getOrganizationId())
                .notificationDate(now)
                .progressive(progressive)
                .status(RewardOrganizationExportStatus.TO_DO)

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
//endregion

    @Test
    void testReserveNextSplitExport() {
        // Given
        RewardOrganizationExport baseExport = buildNewExpectedRewardNotification(RewardNotificationRuleFaker.mockInstance(0), 5);
        baseExport.setExportDate(LocalDate.now());
        baseExport.setRewardsExportedCents(10L);
        baseExport.setRewardsResultsCents(10L);
        baseExport.setRewardNotified(10L);
        baseExport.setRewardsResulted(10L);
        baseExport.setRewardsResultedOk(10L);
        baseExport.setPercentageResulted(10L);
        baseExport.setPercentageResultedOk(10L);
        baseExport.setPercentageResults(10L);
        baseExport.setStatus(RewardOrganizationExportStatus.EXPORTED);

        Mockito.when(rewardOrganizationExportRepositoryMock.save(Mockito.any())).thenAnswer(i -> Mono.just(i.getArgument(0)));

        // When
        RewardOrganizationExport result = service.reserveNextSplitExport(baseExport, 7).block();

        // Then
        Assertions.assertNotNull(result);

        String todayStr = Utils.FORMATTER_DATE.format(LocalDate.now());
        Assertions.assertEquals("ID_0_ssx_%s.12".formatted(todayStr), result.getId());
        Assertions.assertEquals("ORGANIZATION_ID_0_hpd/ID_0_ssx/export/NAME_0_vnj_%s.12.zip".formatted(todayStr), result.getFilePath());
        Assertions.assertEquals(baseExport.getInitiativeId(), result.getInitiativeId());
        Assertions.assertEquals(baseExport.getInitiativeName(), result.getInitiativeName());
        Assertions.assertEquals(baseExport.getOrganizationId(), result.getOrganizationId());
        Assertions.assertEquals(baseExport.getNotificationDate(), result.getNotificationDate());
        Assertions.assertEquals(baseExport.getExportDate(), result.getExportDate());
        Assertions.assertEquals(12, result.getProgressive());
        Assertions.assertEquals(0L, result.getRewardsExportedCents());
        Assertions.assertEquals(0L, result.getRewardsResultsCents());
        Assertions.assertEquals(0L, result.getRewardNotified());
        Assertions.assertEquals(0L, result.getRewardsResulted());
        Assertions.assertEquals(0L, result.getRewardsResultedOk());
        Assertions.assertEquals(0L, result.getPercentageResulted());
        Assertions.assertEquals(0L, result.getPercentageResultedOk());
        Assertions.assertEquals(0L, result.getPercentageResults());
        Assertions.assertEquals(RewardOrganizationExportStatus.IN_PROGRESS, result.getStatus());

        TestUtils.checkNotNullFields(result, "feedbackDate");
    }
}
