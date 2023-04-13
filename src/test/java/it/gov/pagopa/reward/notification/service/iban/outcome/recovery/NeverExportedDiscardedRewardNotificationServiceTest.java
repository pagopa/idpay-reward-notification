package it.gov.pagopa.reward.notification.service.iban.outcome.recovery;

import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardIban;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.test.fakers.RewardsNotificationFaker;
import it.gov.pagopa.reward.notification.utils.ExportCsvConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class NeverExportedDiscardedRewardNotificationServiceTest extends BaseDiscardedRewardNotificationServiceImplTest {

    private NeverExportedDiscardedRewardNotificationService service;

    @BeforeEach
    void init(){
        service = new NeverExportedDiscardedRewardNotificationServiceImpl(rewardsNotificationRepositoryMock, buildReschedulerService());
    }

    @Override
    protected OngoingStubbing<Flux<RewardsNotification>> getRewardNotificationRepoFindDiscardedMockWhen(RewardIban rewardIban) {
        return Mockito.when(rewardsNotificationRepositoryMock.findByUserIdAndInitiativeIdAndStatusAndRejectionReasonAndExportIdNull(
                rewardIban.getUserId(),
                rewardIban.getInitiativeId(),
                RewardNotificationStatus.ERROR,
                ExportCsvConstants.EXPORT_REJECTION_REASON_IBAN_NOT_FOUND));
    }

    @Override
    protected Mono<List<RewardsNotification>> invokeService(RewardIban rewardIban) {
        return service.handleNeverExportedDiscardedRewardNotification(rewardIban);
    }

    @Override
    protected List<RewardsNotification> test(RewardIban rewardIban, RewardNotificationRule rule, LocalDate expectedNotificationDate) {
        // Given
        RewardsNotification discarded = mockIbanErrorNotification(0, rewardIban);

        getRewardNotificationRepoFindDiscardedMockWhen(rewardIban)
                .thenReturn(Flux.just(discarded));

        Mockito.when(notificationRuleServiceMock.findById(rule.getInitiativeId())).thenReturn(Mono.just(rule));

        Mockito.when(rewardsNotificationRepositoryMock.save(Mockito.any())).thenAnswer(i -> Mono.just(i.getArgument(0, RewardsNotification.class)));

        // When
        List<RewardsNotification> result = invokeService(rewardIban).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());

        Mockito.verify(rewardsNotificationRepositoryMock).save(discarded);

        return result;
    }

    private RewardsNotification mockIbanErrorNotification(int bias, RewardIban rewardIban) {
        return RewardsNotificationFaker.mockInstanceBuilder(bias)
                .initiativeId(rewardIban.getInitiativeId())
                .userId(rewardIban.getUserId())
                .status(RewardNotificationStatus.ERROR)
                .exportDate(YESTERDAY.atStartOfDay())
                .resultCode(ExportCsvConstants.EXPORT_REJECTION_REASON_IBAN_NOT_FOUND)
                .rejectionReason(ExportCsvConstants.EXPORT_REJECTION_REASON_IBAN_NOT_FOUND)
                .build();
    }
}
