package it.gov.pagopa.reward.notification.service.rule;

import it.gov.pagopa.reward.notification.dto.rule.InitiativeRefund2StoreDTO;
import it.gov.pagopa.reward.notification.dto.mapper.Initiative2RewardNotificationRuleMapper;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.service.ErrorNotifierService;
import it.gov.pagopa.reward.notification.test.fakers.InitiativeRefundDTOFaker;
import it.gov.pagopa.reward.notification.test.fakers.RewardNotificationRuleFaker;
import it.gov.pagopa.reward.notification.test.utils.TestUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class RefundRuleMediatorServiceTest {
    @Test
    void mediatorTest(){
        // Given
        long commitMillis = 1000;
        Initiative2RewardNotificationRuleMapper initiative2RewardNotificationRuleMapperMock= Mockito.mock(Initiative2RewardNotificationRuleMapper.class);
        RewardNotificationRuleService rewardNotificationRuleServiceMock = Mockito.mock(RewardNotificationRuleService.class);
        ErrorNotifierService errorNotifierServiceMock = Mockito.mock(ErrorNotifierService.class);

        RefundRuleMediatorService mediator = new RefundRuleMediatorServiceImpl(commitMillis,initiative2RewardNotificationRuleMapperMock,rewardNotificationRuleServiceMock,errorNotifierServiceMock, TestUtils.objectMapper);

        InitiativeRefund2StoreDTO initiativeRefund2StoreDTO1 = InitiativeRefundDTOFaker.mockInstance(1);
        InitiativeRefund2StoreDTO initiativeRefund2StoreDTO2 = InitiativeRefundDTOFaker.mockInstance(2);

        Flux<Message<String>> msgs = Flux.just(initiativeRefund2StoreDTO1, initiativeRefund2StoreDTO2)
                .map(TestUtils::jsonSerializer)
                .map(MessageBuilder::withPayload)
                .map(MessageBuilder::build);

        RewardNotificationRule rewardNotificationRuleO1 = RewardNotificationRuleFaker.mockInstance(1);
        Mockito.when(initiative2RewardNotificationRuleMapperMock.apply(initiativeRefund2StoreDTO1)).thenReturn(rewardNotificationRuleO1);
        Mockito.when(initiative2RewardNotificationRuleMapperMock.apply(initiativeRefund2StoreDTO2)).thenThrow(RuntimeException.class);
        Mockito.when(rewardNotificationRuleServiceMock.save(Mockito.any())).thenAnswer(i -> Mono.just(i.getArguments()[0]));

        // When
        mediator.execute(msgs);

        // Then
        Mockito.verify(initiative2RewardNotificationRuleMapperMock, Mockito.times(2)).apply(Mockito.any());
        Mockito.verify(rewardNotificationRuleServiceMock).save(Mockito.any());
        Mockito.verify(errorNotifierServiceMock).notifyRewardNotifierRule(Mockito.any(), Mockito.anyString(), Mockito.anyBoolean(), Mockito.any());

    }
}