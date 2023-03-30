package it.gov.pagopa.reward.notification.service.suspension;

import it.gov.pagopa.reward.notification.connector.wallet.WalletRestClient;
import it.gov.pagopa.reward.notification.exception.ClientExceptionNoBody;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardSuspendedUser;
import it.gov.pagopa.reward.notification.repository.RewardNotificationRuleRepository;
import it.gov.pagopa.reward.notification.repository.RewardsSuspendedUserRepository;
import it.gov.pagopa.reward.notification.test.fakers.RewardNotificationRuleFaker;
import it.gov.pagopa.reward.notification.utils.AuditUtilities;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class UserSuspensionServiceImplTest {
    private static final String ORGANIZATION_ID = "ORGANIZATION_ID";
    private static final String INITIATIVE_ID = "INITIATIVE_ID";
    private static final String USER_ID = "USER_ID";

    @Mock
    private RewardsSuspendedUserRepository rewardsSuspendedUserRepositoryMock;
    @Mock
    private RewardNotificationRuleRepository notificationRuleRepositoryMock;
    @Mock
    private WalletRestClient walletRestClientMock;
    @Mock
    private AuditUtilities auditUtilitiesMock;

    private UserSuspensionService userSuspensionService;

    @BeforeEach
    void init() {
        userSuspensionService = new UserSuspensionServiceImpl(rewardsSuspendedUserRepositoryMock, notificationRuleRepositoryMock, walletRestClientMock, auditUtilitiesMock);
    }

    @Test
    void testInitiativeNotFound() {
        Mockito.when(notificationRuleRepositoryMock.findByInitiativeIdAndOrganizationId(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Mono.empty());

        Executable executable = () -> userSuspensionService.suspend(ORGANIZATION_ID, INITIATIVE_ID, USER_ID).block();

        Assertions.assertThrows(ClientExceptionNoBody.class, executable);

        Mockito.verify(rewardsSuspendedUserRepositoryMock, Mockito.never())
                .findByUserIdAndOrganizationIdAndInitiativeId(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(rewardsSuspendedUserRepositoryMock, Mockito.never())
                .save(Mockito.any(RewardSuspendedUser.class));
        Mockito.verify(walletRestClientMock, Mockito.never())
                .suspend(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void testUserAlreadySuspended() {
        RewardNotificationRule notificationRule = RewardNotificationRuleFaker.mockInstance(1);
        Mockito.when(notificationRuleRepositoryMock.findByInitiativeIdAndOrganizationId(INITIATIVE_ID, ORGANIZATION_ID))
                .thenReturn(Mono.just(notificationRule));

        RewardSuspendedUser alreadySuspended = new RewardSuspendedUser(USER_ID, INITIATIVE_ID, ORGANIZATION_ID);
        Mockito.when(rewardsSuspendedUserRepositoryMock.findByUserIdAndOrganizationIdAndInitiativeId(USER_ID, ORGANIZATION_ID, INITIATIVE_ID))
                .thenReturn(Mono.just(alreadySuspended));

        Mockito.when(rewardsSuspendedUserRepositoryMock.save(Mockito.any(RewardSuspendedUser.class)))
                .thenAnswer(i -> Mono.just(i.getArgument(0)));

        userSuspensionService.suspend(ORGANIZATION_ID, INITIATIVE_ID, USER_ID).block();

        Mockito.verify(walletRestClientMock, Mockito.never())
                .suspend(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void testOk() {
        RewardNotificationRule notificationRule = RewardNotificationRuleFaker.mockInstance(1);
        Mockito.when(notificationRuleRepositoryMock.findByInitiativeIdAndOrganizationId(INITIATIVE_ID, ORGANIZATION_ID))
                .thenReturn(Mono.just(notificationRule));

        Mockito.when(rewardsSuspendedUserRepositoryMock.findByUserIdAndOrganizationIdAndInitiativeId(USER_ID, ORGANIZATION_ID, INITIATIVE_ID))
                .thenReturn(Mono.empty());
        RewardSuspendedUser expected = new RewardSuspendedUser(USER_ID, INITIATIVE_ID, ORGANIZATION_ID);
        Mockito.when(rewardsSuspendedUserRepositoryMock.save(Mockito.any(RewardSuspendedUser.class))).thenReturn(Mono.just(expected));

        Mockito.when(walletRestClientMock.suspend(INITIATIVE_ID, USER_ID)).thenReturn(Mono.just(ResponseEntity.ok().build()));

        userSuspensionService.suspend(ORGANIZATION_ID, INITIATIVE_ID, USER_ID).block();

        Mockito.verify(rewardsSuspendedUserRepositoryMock, Mockito.never())
                .deleteById(Mockito.anyString());
    }

    @Test
    void testKo() {
        RewardNotificationRule notificationRule = RewardNotificationRuleFaker.mockInstance(1);
        Mockito.when(notificationRuleRepositoryMock.findByInitiativeIdAndOrganizationId(INITIATIVE_ID, ORGANIZATION_ID))
                .thenReturn(Mono.just(notificationRule));

        Mockito.when(rewardsSuspendedUserRepositoryMock.findByUserIdAndOrganizationIdAndInitiativeId(USER_ID, ORGANIZATION_ID, INITIATIVE_ID))
                .thenReturn(Mono.empty());
        RewardSuspendedUser expected = new RewardSuspendedUser(USER_ID, INITIATIVE_ID, ORGANIZATION_ID);
        Mockito.when(rewardsSuspendedUserRepositoryMock.save(Mockito.any(RewardSuspendedUser.class))).thenReturn(Mono.just(expected));

        Mockito.when(walletRestClientMock.suspend(INITIATIVE_ID, USER_ID)).thenReturn(Mono.error(new ClientExceptionNoBody(HttpStatus.INTERNAL_SERVER_ERROR)));

        Mockito.when(rewardsSuspendedUserRepositoryMock.deleteById(RewardSuspendedUser.buildId(USER_ID, INITIATIVE_ID))).thenReturn(Mono.empty());

        Executable executable = () -> userSuspensionService.suspend(ORGANIZATION_ID, INITIATIVE_ID, USER_ID).block();

        Assertions.assertThrows(ClientExceptionNoBody.class, executable);

        Mockito.verify(rewardsSuspendedUserRepositoryMock).deleteById(RewardSuspendedUser.buildId(USER_ID, INITIATIVE_ID));
    }

    @Test
    void testNotSuspendedUser() {
        Mockito.when(rewardsSuspendedUserRepositoryMock.existsById(Mockito.anyString())).thenReturn(Mono.just(Boolean.FALSE));

        Boolean result = userSuspensionService.isNotSuspendedUser(INITIATIVE_ID, USER_ID).block();

        Assertions.assertEquals(Boolean.TRUE, result);
    }
}