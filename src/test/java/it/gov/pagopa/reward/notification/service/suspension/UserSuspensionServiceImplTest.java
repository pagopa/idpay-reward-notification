package it.gov.pagopa.reward.notification.service.suspension;

import it.gov.pagopa.reward.notification.connector.wallet.WalletRestClient;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.exception.ClientExceptionNoBody;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardSuspendedUser;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardNotificationRuleRepository;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.repository.RewardsSuspendedUserRepository;
import it.gov.pagopa.reward.notification.service.RewardsNotificationDateReschedulerService;
import it.gov.pagopa.reward.notification.test.fakers.RewardNotificationRuleFaker;
import it.gov.pagopa.reward.notification.test.fakers.RewardsNotificationFaker;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Collections;

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
    private RewardsNotificationRepository rewardsNotificationRepositoryMock;
    @Mock
    private RewardsNotificationDateReschedulerService dateHandlerServiceMock;
    @Mock
    private WalletRestClient walletRestClientMock;
    @Mock
    private AuditUtilities auditUtilitiesMock;

    private UserSuspensionService userSuspensionService;

    @BeforeEach
    void init() {
        userSuspensionService = new UserSuspensionServiceImpl(rewardsSuspendedUserRepositoryMock,
                notificationRuleRepositoryMock,
                rewardsNotificationRepositoryMock,
                dateHandlerServiceMock,
                walletRestClientMock,
                auditUtilitiesMock);
    }

    //region suspension
    @Test
    void testSuspensionInitiativeNotFound() {
        Mockito.when(notificationRuleRepositoryMock.findByInitiativeIdAndOrganizationId(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Mono.empty());

        RewardSuspendedUser result = userSuspensionService.suspend(ORGANIZATION_ID, INITIATIVE_ID, USER_ID).block();

        Assertions.assertNull(result);

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

        RewardSuspendedUser result = userSuspensionService.suspend(ORGANIZATION_ID, INITIATIVE_ID, USER_ID).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(alreadySuspended, result);

        Mockito.verify(walletRestClientMock, Mockito.never())
                .suspend(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void testSuspensionOk() {
        RewardNotificationRule notificationRule = RewardNotificationRuleFaker.mockInstance(1);
        Mockito.when(notificationRuleRepositoryMock.findByInitiativeIdAndOrganizationId(INITIATIVE_ID, ORGANIZATION_ID))
                .thenReturn(Mono.just(notificationRule));

        Mockito.when(rewardsSuspendedUserRepositoryMock.findByUserIdAndOrganizationIdAndInitiativeId(USER_ID, ORGANIZATION_ID, INITIATIVE_ID))
                .thenReturn(Mono.empty());
        RewardSuspendedUser expected = new RewardSuspendedUser(USER_ID, INITIATIVE_ID, ORGANIZATION_ID);
        Mockito.when(rewardsSuspendedUserRepositoryMock.save(Mockito.any(RewardSuspendedUser.class))).thenReturn(Mono.just(expected));

        Mockito.when(walletRestClientMock.suspend(INITIATIVE_ID, USER_ID)).thenReturn(Mono.just(ResponseEntity.ok().build()));

        RewardSuspendedUser result = userSuspensionService.suspend(ORGANIZATION_ID, INITIATIVE_ID, USER_ID).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(expected, result);

        Mockito.verify(rewardsSuspendedUserRepositoryMock, Mockito.never())
                .deleteById(Mockito.anyString());
    }

    @Test
    void testSuspensionKo() {
        RewardNotificationRule notificationRule = RewardNotificationRuleFaker.mockInstance(1);
        Mockito.when(notificationRuleRepositoryMock.findByInitiativeIdAndOrganizationId(INITIATIVE_ID, ORGANIZATION_ID))
                .thenReturn(Mono.just(notificationRule));

        Mockito.when(rewardsSuspendedUserRepositoryMock.findByUserIdAndOrganizationIdAndInitiativeId(USER_ID, ORGANIZATION_ID, INITIATIVE_ID))
                .thenReturn(Mono.empty());
        RewardSuspendedUser expected = new RewardSuspendedUser(USER_ID, INITIATIVE_ID, ORGANIZATION_ID);
        Mockito.when(rewardsSuspendedUserRepositoryMock.save(Mockito.any(RewardSuspendedUser.class))).thenReturn(Mono.just(expected));

        Mockito.when(walletRestClientMock.suspend(INITIATIVE_ID, USER_ID)).thenReturn(Mono.error(new ClientExceptionNoBody(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR")));

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
    //endregion

    //region readmission
    @Test
    void testReadmissionInitiativeNotFound() {
        Mockito.when(notificationRuleRepositoryMock.findByInitiativeIdAndOrganizationId(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Mono.empty());

        RewardSuspendedUser result = userSuspensionService.readmit(ORGANIZATION_ID, INITIATIVE_ID, USER_ID).block();

        Assertions.assertNull(result);

        Mockito.verify(rewardsSuspendedUserRepositoryMock, Mockito.never())
                .findByUserIdAndOrganizationIdAndInitiativeId(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(rewardsSuspendedUserRepositoryMock, Mockito.never())
                .delete(Mockito.any(RewardSuspendedUser.class));
        Mockito.verify(walletRestClientMock, Mockito.never())
                .readmit(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void testUserAlreadyActive() {
        RewardNotificationRule notificationRule = RewardNotificationRuleFaker.mockInstance(1);
        Mockito.when(notificationRuleRepositoryMock.findByInitiativeIdAndOrganizationId(INITIATIVE_ID, ORGANIZATION_ID))
                .thenReturn(Mono.just(notificationRule));

        Mockito.when(rewardsSuspendedUserRepositoryMock.findByUserIdAndOrganizationIdAndInitiativeId(USER_ID, ORGANIZATION_ID, INITIATIVE_ID))
                .thenReturn(Mono.empty());

        RewardSuspendedUser result = userSuspensionService.readmit(ORGANIZATION_ID, INITIATIVE_ID, USER_ID).block();

        Assertions.assertNotNull(result);

        Assertions.assertEquals(new RewardSuspendedUser(), result);

        Mockito.verify(rewardsSuspendedUserRepositoryMock, Mockito.never())
                .delete(Mockito.any(RewardSuspendedUser.class));
        Mockito.verify(walletRestClientMock, Mockito.never())
                .readmit(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void testReadmissionOk() {
        LocalDate now = LocalDate.now();

        RewardNotificationRule notificationRule = RewardNotificationRuleFaker.mockInstance(1);
        Mockito.when(notificationRuleRepositoryMock.findByInitiativeIdAndOrganizationId(INITIATIVE_ID, ORGANIZATION_ID))
                .thenReturn(Mono.just(notificationRule));

        RewardSuspendedUser suspendedUser = new RewardSuspendedUser(USER_ID, INITIATIVE_ID, ORGANIZATION_ID);
        Mockito.when(rewardsSuspendedUserRepositoryMock.findByUserIdAndOrganizationIdAndInitiativeId(USER_ID, ORGANIZATION_ID, INITIATIVE_ID))
                .thenReturn(Mono.just(suspendedUser));

        Mockito.when(rewardsSuspendedUserRepositoryMock.delete(Mockito.any(RewardSuspendedUser.class))).thenReturn(Mono.empty());

        RewardsNotification notification1 = RewardsNotificationFaker.mockInstanceBuilder(1)
                .userId(USER_ID)
                .initiativeId(INITIATIVE_ID)
                .organizationId(ORGANIZATION_ID)
                .status(RewardNotificationStatus.SUSPENDED)
                .notificationDate(now.minusDays(3))
                .build();
        Mockito.when(rewardsNotificationRepositoryMock.findByUserIdAndInitiativeIdAndStatus(USER_ID, INITIATIVE_ID, RewardNotificationStatus.SUSPENDED))
                .thenReturn(Flux.just(notification1));

        RewardsNotification notification2 = notification1.toBuilder().notificationDate(now).build();
        Mockito.when(dateHandlerServiceMock.setHandledNotificationDate(notificationRule, notification1))
                        .thenReturn(Mono.just(notification2));

        Mockito.when(rewardsNotificationRepositoryMock.save(Mockito.any(RewardsNotification.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

        Mockito.when(walletRestClientMock.readmit(INITIATIVE_ID, USER_ID)).thenReturn(Mono.just(ResponseEntity.ok().build()));

        RewardSuspendedUser result = userSuspensionService.readmit(ORGANIZATION_ID, INITIATIVE_ID, USER_ID).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(suspendedUser, result);
        Assertions.assertEquals(RewardNotificationStatus.TO_SEND, notification2.getStatus());
        Assertions.assertTrue(notification2.getNotificationDate().isAfter(notification1.getNotificationDate()));

        Mockito.verify(rewardsSuspendedUserRepositoryMock, Mockito.never()).save(Mockito.any(RewardSuspendedUser.class));
    }

    @Test
    void testReadmissionKo() {
        RewardNotificationRule notificationRule = RewardNotificationRuleFaker.mockInstance(1);
        Mockito.when(notificationRuleRepositoryMock.findByInitiativeIdAndOrganizationId(INITIATIVE_ID, ORGANIZATION_ID))
                .thenReturn(Mono.just(notificationRule));

        RewardSuspendedUser expected = new RewardSuspendedUser(USER_ID, INITIATIVE_ID, ORGANIZATION_ID);
        Mockito.when(rewardsSuspendedUserRepositoryMock.findByUserIdAndOrganizationIdAndInitiativeId(USER_ID, ORGANIZATION_ID, INITIATIVE_ID))
                .thenReturn(Mono.just(expected));
        Mockito.when(rewardsSuspendedUserRepositoryMock.delete(expected)).thenReturn(Mono.empty());

        Mockito.when(rewardsNotificationRepositoryMock.findByUserIdAndInitiativeIdAndStatus(USER_ID, INITIATIVE_ID, RewardNotificationStatus.SUSPENDED))
                .thenReturn(Flux.fromIterable(Collections.emptyList()));

        Mockito.when(walletRestClientMock.readmit(INITIATIVE_ID, USER_ID)).thenReturn(Mono.error(new ClientExceptionNoBody(HttpStatus.INTERNAL_SERVER_ERROR)));

        Mockito.when(rewardsSuspendedUserRepositoryMock.save(Mockito.any(RewardSuspendedUser.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

        Executable executable = () -> userSuspensionService.readmit(ORGANIZATION_ID, INITIATIVE_ID, USER_ID).block();

        Assertions.assertThrows(ClientExceptionNoBody.class, executable);

        Mockito.verify(rewardsSuspendedUserRepositoryMock).save(Mockito.any(RewardSuspendedUser.class));
    }
    //endregion
}