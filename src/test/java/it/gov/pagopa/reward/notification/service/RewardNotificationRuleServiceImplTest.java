package it.gov.pagopa.reward.notification.service;

import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.repository.RewardNotificationRuleRepository;
import it.gov.pagopa.reward.notification.test.fakers.RewardNotificationRuleFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class RewardNotificationRuleServiceImplTest {

    @Mock
    private RewardNotificationRuleRepository rewardNotificationRuleRepositoryMock;

    private RewardNotificationRuleService rewardNotificationRuleService;

    @BeforeEach
    void setUp() {
        rewardNotificationRuleService = new RewardNotificationRuleServiceImpl(rewardNotificationRuleRepositoryMock);
    }

    @Test
    void save() {
        // Given
        RewardNotificationRule rewardNotificationRule = RewardNotificationRuleFaker.mockInstance(1);

        Mockito.when(rewardNotificationRuleRepositoryMock.save(rewardNotificationRule)).thenAnswer(i -> Mono.just(i.getArguments()[0]));

        // When
        RewardNotificationRule result = rewardNotificationRuleService.save(rewardNotificationRule).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(result, rewardNotificationRule);
    }
}