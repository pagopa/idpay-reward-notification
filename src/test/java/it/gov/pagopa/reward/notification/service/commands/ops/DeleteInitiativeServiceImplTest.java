package it.gov.pagopa.reward.notification.service.commands.ops;

import com.mongodb.MongoException;
import it.gov.pagopa.reward.notification.model.*;
import it.gov.pagopa.reward.notification.repository.*;
import it.gov.pagopa.reward.notification.test.fakers.RewardNotificationRuleFaker;
import it.gov.pagopa.reward.notification.test.fakers.RewardOrganizationExportsFaker;
import it.gov.pagopa.reward.notification.test.fakers.RewardOrganizationImportFaker;
import it.gov.pagopa.reward.notification.test.fakers.RewardsNotificationFaker;
import it.gov.pagopa.reward.notification.utils.AuditUtilities;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@ExtendWith(MockitoExtension.class)
class DeleteInitiativeServiceImplTest {
    @Mock private RewardNotificationRuleRepository rewardNotificationRuleRepositoryMock;
    @Mock private RewardOrganizationExportsRepository rewardOrganizationExportsRepositoryMock;
    @Mock private RewardOrganizationImportsRepository rewardOrganizationImportsRepositoryMock;
    @Mock private RewardsNotificationRepository rewardsNotificationRepositoryMock;
    @Mock private RewardIbanRepository rewardIbanRepositoryMock;
    @Mock private RewardsRepository rewardsRepositoryMock;
    @Mock private RewardsSuspendedUserRepository rewardsSuspendedUserRepositoryMock;
    @Mock private AuditUtilities auditUtilitiesMock;

    private DeleteInitiativeService deleteInitiativeService;

    @BeforeEach
    void setUp() {
        deleteInitiativeService = new DeleteInitiativeServiceImpl(
                rewardNotificationRuleRepositoryMock,
                rewardOrganizationExportsRepositoryMock,
                rewardOrganizationImportsRepositoryMock,
                rewardsNotificationRepositoryMock,
                rewardIbanRepositoryMock,
                rewardsRepositoryMock,
                rewardsSuspendedUserRepositoryMock,
                auditUtilitiesMock);
    }

    @Test
    void executeOK() {
        String initiativeId = "INITIATIVEID";
        String userid = "USERID";
        String id = "ID";
        int pageSize = 2;
        long delay = 1;

        RewardNotificationRule rewardNotificationRule = RewardNotificationRuleFaker.mockInstanceBuilder(1)
                .initiativeId(initiativeId)
                .build();
        Mockito.when(rewardNotificationRuleRepositoryMock.findByIdWithBatch(initiativeId,pageSize))
                .thenReturn(Flux.just(rewardNotificationRule));
        Mockito.when(rewardNotificationRuleRepositoryMock.deleteById(rewardNotificationRule.getInitiativeId()))
                .thenReturn(Mono.empty());

        RewardOrganizationExport rewardOrganizationExport = RewardOrganizationExportsFaker.mockInstanceBuilder(1)
                .initiativeId(initiativeId)
                .filePath(null)
                .build();
        Mockito.when(rewardOrganizationExportsRepositoryMock.deleteByInitiativeId(initiativeId))
                .thenReturn(Flux.just(rewardOrganizationExport));

        RewardOrganizationImport rewardOrganizationImport = RewardOrganizationImportFaker.mockInstance(1);
        rewardOrganizationImport.setInitiativeId(initiativeId);
        Mockito.when(rewardOrganizationImportsRepositoryMock.findByInitiativeIdWithBatch(initiativeId, pageSize))
                .thenReturn(Flux.just(rewardOrganizationImport));
        Mockito.when(rewardOrganizationImportsRepositoryMock.deleteById(rewardOrganizationImport.getFilePath()))
                .thenReturn(Mono.empty());

        RewardsNotification rewardsNotification = RewardsNotificationFaker
                .mockInstanceBuilder(1, initiativeId, LocalDate.now())
                .build();
        Mockito.when(rewardsNotificationRepositoryMock.findByInitiativeIdWithBatch(initiativeId, pageSize))
                .thenReturn(Flux.just(rewardsNotification));
        Mockito.when(rewardsNotificationRepositoryMock.deleteById(rewardsNotification.getId()))
                .thenReturn(Mono.empty());

        RewardIban rewardIban = RewardIban.builder()
                .id(id)
                .userId(userid)
                .initiativeId(initiativeId)
                .build();
        Mockito.when(rewardIbanRepositoryMock.findByInitiativeIdWithBatch(initiativeId, pageSize))
                .thenReturn(Flux.just(rewardIban));
        Mockito.when(rewardIbanRepositoryMock.deleteById(rewardIban.getId()))
                .thenReturn(Mono.empty());

        Rewards rewards = Rewards.builder()
                .id(id)
                .userId(userid)
                .initiativeId(initiativeId)
                .build();
        Mockito.when(rewardsRepositoryMock.findByInitiativeIdWithBatch(initiativeId, pageSize))
                .thenReturn(Flux.just(rewards));
        Mockito.when(rewardsRepositoryMock.deleteById(rewards.getId()))
                .thenReturn(Mono.empty());

        RewardSuspendedUser rewardSuspendedUser = RewardSuspendedUser.builder()
                .id(id)
                .userId(userid)
                .initiativeId(initiativeId)
                .build();
        Mockito.when(rewardsSuspendedUserRepositoryMock.findByInitiativeIdWithBatch(initiativeId, pageSize))
                .thenReturn(Flux.just(rewardSuspendedUser));
        Mockito.when(rewardsSuspendedUserRepositoryMock.deleteById(rewardSuspendedUser.getId()))
                .thenReturn(Mono.empty());

        String result = deleteInitiativeService.execute(initiativeId, pageSize, delay).block();

        Assertions.assertNotNull(result);

        Mockito.verify(rewardNotificationRuleRepositoryMock, Mockito.times(1)).findByIdWithBatch(Mockito.anyString(), Mockito.anyInt());
        Mockito.verify(rewardNotificationRuleRepositoryMock, Mockito.times(1)).deleteById(Mockito.anyString());
        Mockito.verify(rewardOrganizationExportsRepositoryMock, Mockito.times(1)).deleteByInitiativeId(Mockito.anyString());
        Mockito.verify(rewardOrganizationImportsRepositoryMock, Mockito.times(1)).findByInitiativeIdWithBatch(Mockito.anyString(), Mockito.anyInt());
        Mockito.verify(rewardOrganizationImportsRepositoryMock, Mockito.times(1)).deleteById(Mockito.anyString());
        Mockito.verify(rewardsNotificationRepositoryMock, Mockito.times(1)).findByInitiativeIdWithBatch(Mockito.anyString(), Mockito.anyInt());
        Mockito.verify(rewardsNotificationRepositoryMock, Mockito.times(1)).deleteById(Mockito.anyString());
        Mockito.verify(rewardIbanRepositoryMock, Mockito.times(1)).findByInitiativeIdWithBatch(Mockito.anyString(), Mockito.anyInt());
        Mockito.verify(rewardIbanRepositoryMock, Mockito.times(1)).deleteById(Mockito.anyString());
        Mockito.verify(rewardsRepositoryMock, Mockito.times(1)).findByInitiativeIdWithBatch(Mockito.anyString(), Mockito.anyInt());
        Mockito.verify(rewardsRepositoryMock, Mockito.times(1)).deleteById(Mockito.anyString());
        Mockito.verify(rewardsSuspendedUserRepositoryMock, Mockito.times(1)).findByInitiativeIdWithBatch(Mockito.anyString(), Mockito.anyInt());
        Mockito.verify(rewardsSuspendedUserRepositoryMock, Mockito.times(1)).deleteById(Mockito.anyString());
    }

    @Test
    void executeError() {
        String initiativeId = "INITIATIVEID";
        int pageSize = 2;
        long delay = 1;
        Mockito.when(rewardNotificationRuleRepositoryMock.findByIdWithBatch(initiativeId, pageSize))
                .thenThrow(new MongoException("DUMMY_EXCEPTION"));

        try{
            deleteInitiativeService.execute(initiativeId, pageSize, delay).block();
            Assertions.fail();
        }catch (Throwable t){
            Assertions.assertTrue(t instanceof  MongoException);
        }
    }
}