package it.gov.pagopa.reward.notification.service.csv.export;

import it.gov.pagopa.reward.notification.enums.ExportStatus;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.repository.RewardNotificationRuleRepository;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationExportsRepository;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.utils.Utils;
import it.gov.pagopa.reward.notification.test.fakers.RewardNotificationRuleFaker;
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
        Mockito.when(rewardsNotificationRepositoryMock.findInitiatives2Notify()).thenReturn(Flux.empty());

        RewardOrganizationExport result = service.retrieve().block();
        Assertions.assertSame(expectedResult, result);

        Mockito.verifyNoMoreInteractions(rewardOrganizationExportRepositoryMock, rewardsNotificationRepositoryMock, rewardNotificationRuleRepositoryMock);
    }

    @Test
    void testWhenReservationNotExistsAndNotRewards2Notify() {
        Mockito.when(rewardOrganizationExportRepositoryMock.reserveExport()).thenReturn(Mono.empty());
        Mockito.when(rewardsNotificationRepositoryMock.findInitiatives2Notify()).thenReturn(Flux.empty());

        RewardOrganizationExport result = service.retrieve().block();
        Assertions.assertNull(result);

        Mockito.verifyNoMoreInteractions(rewardOrganizationExportRepositoryMock, rewardsNotificationRepositoryMock, rewardNotificationRuleRepositoryMock);
    }

    @Test
    void testWhenReservationNotExists() {
        Mockito.when(rewardsNotificationRepositoryMock.findInitiatives2Notify()).thenReturn(Flux.just("INITIATIVEID1", "INITIATIVEID2", "INITIATIVEID3"));

        RewardNotificationRule rule1 = RewardNotificationRuleFaker.mockInstance(1);
        RewardNotificationRule rule2 = RewardNotificationRuleFaker.mockInstance(2);
        RewardNotificationRule rule3 = RewardNotificationRuleFaker.mockInstance(3);

        Mockito.when(rewardNotificationRuleRepositoryMock.findById("INITIATIVEID1")).thenReturn(Mono.just(rule1));
        Mockito.when(rewardNotificationRuleRepositoryMock.findById("INITIATIVEID2")).thenReturn(Mono.just(rule2));
        Mockito.when(rewardNotificationRuleRepositoryMock.findById("INITIATIVEID3")).thenReturn(Mono.just(rule3));

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
                                .initiativeId("INITIATIVEID3")
                                .notificationDate(now)
                                .build())))
                .thenReturn(Mono.just(0L));

        RewardOrganizationExport expectedNewRewardNotification1 = buildNewExpectedRewardNotification(rule1, 1L);
        RewardOrganizationExport expectedNewRewardNotification2 = buildNewExpectedRewardNotification(rule2, 3L);
        RewardOrganizationExport expectedNewRewardNotification3 = buildNewExpectedRewardNotification(rule3, 1L);

        Mockito.when(rewardOrganizationExportRepositoryMock.configureNewExport(expectedNewRewardNotification1)).thenReturn(Mono.just(expectedNewRewardNotification1));
        Mockito.when(rewardOrganizationExportRepositoryMock.configureNewExport(expectedNewRewardNotification2)).thenReturn(Mono.just(expectedNewRewardNotification2));
        Mockito.when(rewardOrganizationExportRepositoryMock.configureNewExport(expectedNewRewardNotification3)).thenReturn(Mono.empty());

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
                .build();
    }
}
