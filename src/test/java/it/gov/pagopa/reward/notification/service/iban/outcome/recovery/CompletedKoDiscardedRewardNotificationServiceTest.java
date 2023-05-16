package it.gov.pagopa.reward.notification.service.iban.outcome.recovery;

import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardIban;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.test.fakers.RewardsNotificationFaker;
import org.apache.commons.lang3.ObjectUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class CompletedKoDiscardedRewardNotificationServiceTest extends BaseDiscardedRewardNotificationServiceImplTest {

    private CompletedKoDiscardedRewardNotificationService service;

    @BeforeEach
    void init(){
        service = new CompletedKoDiscardedRewardNotificationServiceImpl(rewardsNotificationRepositoryMock, buildReschedulerService());
    }

    @Override
    protected OngoingStubbing<Flux<RewardsNotification>> getRewardNotificationRepoFindDiscardedMockWhen(RewardIban rewardIban) {
        return Mockito.when(rewardsNotificationRepositoryMock.findByBeneficiaryIdAndInitiativeIdAndStatusAndRemedialIdNull(rewardIban.getUserId(),
                        rewardIban.getInitiativeId(),
                        RewardNotificationStatus.COMPLETED_KO));
    }

    @Override
    protected Mono<List<RewardsNotification>> invokeService(RewardIban rewardIban) {
        return service.handleCompletedKoDiscardedRewardNotification(rewardIban);
    }

    @Test
    @Override
    void testWhenSaveError() {
        Mockito.when(rewardsNotificationRepositoryMock.saveIfNotExists(Mockito.any())).thenReturn(Mono.empty());
        super.testWhenSaveError();
    }

    @Override
    protected List<RewardsNotification> test(RewardIban rewardIban, RewardNotificationRule rule, LocalDate expectedNotificationDate) {
        // Given
        RewardsNotification recovered1 = mockCompletedKoNotification(0, rewardIban);
        RewardsNotification recovered2 = mockCompletedKoNotification(1, rewardIban);

        getRewardNotificationRepoFindDiscardedMockWhen(rewardIban)
                .thenReturn(Flux.just(recovered1,recovered2));

        Mockito.when(notificationRuleServiceMock.findById(rule.getInitiativeId())).thenReturn(Mono.just(rule));

        Mockito.when(rewardsNotificationRepositoryMock.save(Mockito.any())).thenAnswer(i -> Mono.just(i.getArgument(0, RewardsNotification.class)));

        Mockito.when(rewardsNotificationRepositoryMock.saveIfNotExists(Mockito.argThat(r->r.getId().endsWith("recovery-1")))).thenAnswer(i -> Mono.just(i.getArgument(0, RewardsNotification.class)));
        Mockito.doReturn(Mono.empty())
                .when(rewardsNotificationRepositoryMock).saveIfNotExists(Mockito.argThat(r->r.getId().endsWith("recovery-2"))); // if already stored

        // When
        List<RewardsNotification> result = invokeService(rewardIban).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size());

        Mockito.verify(rewardsNotificationRepositoryMock).save(recovered1);
        Mockito.verify(rewardsNotificationRepositoryMock).save(recovered2);
        result.forEach(r ->
                Mockito.verify(rewardsNotificationRepositoryMock).saveIfNotExists(r)
        );

        verifyRecovered(recovered1, "_recovery-1");

        verifyRecovered(recovered2, "_recovery-2");

        verifyRemedial(expectedNotificationDate, recovered1, result.get(0), "_recovery-1");
        verifyRemedial(expectedNotificationDate, recovered2, result.get(1), "_recovery-2");

        return result;
    }

    private static void verifyRecovered(RewardsNotification recovered, String remedialSuffix) {
        Assertions.assertEquals(RewardNotificationStatus.RECOVERED, recovered.getStatus());
        String expectedRemedialId;
        if(recovered.getId().contains("_recovery-")){
            expectedRemedialId = recovered.getId().replaceAll("_recovery-[0-9]+$", remedialSuffix);
        } else {
            expectedRemedialId = recovered.getId() + remedialSuffix;
        }
        Assertions.assertEquals(expectedRemedialId, recovered.getRemedialId());
    }

    private static void verifyRemedial(LocalDate expectedNotificationDate, RewardsNotification recovered, RewardsNotification result, String remedialSuffix) {
        String expectedRemedialId;
        String expectedRemedialExternalId;
        if(recovered.getId().contains("_recovery-")){
            expectedRemedialId = recovered.getId().replaceAll("_recovery-[0-9]+$", remedialSuffix);
            expectedRemedialExternalId = recovered.getExternalId().replaceAll("_recovery-[0-9]+$", remedialSuffix);
        } else {
            expectedRemedialId = recovered.getId() + remedialSuffix;
            expectedRemedialExternalId = recovered.getExternalId() + remedialSuffix;
        }

        Assertions.assertNull(result.getRemedialId());
        Assertions.assertEquals(expectedRemedialId, result.getId());
        Assertions.assertEquals(expectedRemedialExternalId, result.getExternalId());
        Assertions.assertEquals(ObjectUtils.firstNonNull(recovered.getOrdinaryId(), recovered.getId()), result.getOrdinaryId());
        Assertions.assertEquals(ObjectUtils.firstNonNull(recovered.getOrdinaryExternalId(), recovered.getExternalId()), result.getOrdinaryExternalId());
        Assertions.assertEquals(recovered.getId(), result.getRecoveredId());
        Assertions.assertEquals(recovered.getExternalId(), result.getRecoveredExternalId());
        verifyRemedialStatusFields(result, expectedNotificationDate);
    }

    private static RewardsNotification mockCompletedKoNotification(int bias, RewardIban rewardIban) {
        RewardsNotification out = RewardsNotificationFaker.mockInstanceBuilder(bias)
                .initiativeId(rewardIban.getInitiativeId())
                .beneficiaryId(rewardIban.getUserId())
                .status(RewardNotificationStatus.COMPLETED_KO)
                .exportDate(YESTERDAY.atStartOfDay())
                .resultCode("RESULTCODE")
                .rejectionReason("REJECTIONREASON")
                .feedbackDate(TODAY.atStartOfDay())
                .build();

        if(bias % 2 == 1){
            out.setOrdinaryId(out.getId());
            out.setOrdinaryExternalId(out.getExternalId());
            out.setId(out.getId()+"_recovery-1");
            out.setExternalId(out.getExternalId()+"_recovery-1");
        }

        return out;
    }
}
