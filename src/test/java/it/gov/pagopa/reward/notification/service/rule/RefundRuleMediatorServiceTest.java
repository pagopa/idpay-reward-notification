package it.gov.pagopa.reward.notification.service.rule;

import it.gov.pagopa.reward.notification.dto.rule.InitiativeRefund2StoreDTO;
import it.gov.pagopa.reward.notification.dto.mapper.Initiative2RewardNotificationRuleMapper;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.service.ErrorNotifierService;
import it.gov.pagopa.reward.notification.service.ErrorNotifierServiceImpl;
import it.gov.pagopa.reward.notification.test.fakers.InitiativeRefundDTOFaker;
import it.gov.pagopa.reward.notification.test.fakers.RewardNotificationRuleFaker;
import it.gov.pagopa.reward.notification.test.utils.TestUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

class RefundRuleMediatorServiceTest {
    @Test
    void mediatorTest(){
        // Given
        long commitMillis = 1000;
        Initiative2RewardNotificationRuleMapper initiative2RewardNotificationRuleMapperMock= Mockito.mock(Initiative2RewardNotificationRuleMapper.class);
        RewardNotificationRuleService rewardNotificationRuleServiceMock = Mockito.mock(RewardNotificationRuleService.class);
        ErrorNotifierService errorNotifierServiceMock = Mockito.mock(ErrorNotifierService.class);
        RewardNotificationRuleValidatorService rewardNotificationRuleValidatorServiceMock = Mockito.mock(RewardNotificationRuleValidatorService.class);

        RefundRuleMediatorService mediator = new RefundRuleMediatorServiceImpl("appName", commitMillis,initiative2RewardNotificationRuleMapperMock,rewardNotificationRuleValidatorServiceMock, rewardNotificationRuleServiceMock,errorNotifierServiceMock, TestUtils.objectMapper);

        InitiativeRefund2StoreDTO initiativeRefund2StoreDTO1 = InitiativeRefundDTOFaker.mockInstance(1);
        InitiativeRefund2StoreDTO initiativeRefund2StoreDTO2 = InitiativeRefundDTOFaker.mockInstance(2);
        InitiativeRefund2StoreDTO initiativeRefund2StoreDTO3 = InitiativeRefundDTOFaker.mockInstance(3);

        Flux<Message<String>> msgs = Flux.just(initiativeRefund2StoreDTO1, initiativeRefund2StoreDTO2, initiativeRefund2StoreDTO3)
                .map(TestUtils::jsonSerializer)
                .map(payload -> MessageBuilder
                        .withPayload(payload)
                        .setHeader(KafkaHeaders.RECEIVED_PARTITION_ID, 0)
                        .setHeader(KafkaHeaders.OFFSET, 0L)
                )
                .map(MessageBuilder::build);

        RewardNotificationRule rewardNotificationRuleO1 = RewardNotificationRuleFaker.mockInstance(1);
        RewardNotificationRule rewardNotificationRuleO3 = RewardNotificationRuleFaker.mockInstance(3);

        Mockito.when(initiative2RewardNotificationRuleMapperMock.apply(initiativeRefund2StoreDTO1)).thenReturn(rewardNotificationRuleO1);
        Mockito.when(initiative2RewardNotificationRuleMapperMock.apply(initiativeRefund2StoreDTO2)).thenThrow(RuntimeException.class);
        Mockito.when(initiative2RewardNotificationRuleMapperMock.apply(initiativeRefund2StoreDTO3)).thenReturn(rewardNotificationRuleO3);
        Mockito.doThrow(new IllegalArgumentException("INVALIDRULE")).when(rewardNotificationRuleValidatorServiceMock).validate(rewardNotificationRuleO3);
        Mockito.when(rewardNotificationRuleServiceMock.save(Mockito.any())).thenAnswer(i -> Mono.just(i.getArguments()[0]));

        // When
        mediator.execute(msgs);

        // Then
        Mockito.verify(initiative2RewardNotificationRuleMapperMock, Mockito.times(3)).apply(Mockito.any());
        Mockito.verify(rewardNotificationRuleServiceMock).save(Mockito.any());
        Mockito.verify(errorNotifierServiceMock, Mockito.times(2)).notifyRewardNotifierRule(Mockito.any(), Mockito.anyString(), Mockito.anyBoolean(), Mockito.any());

    }

    @Test
    void otherApplicationRetryTest(){
        // Given
        Initiative2RewardNotificationRuleMapper initiative2RewardNotificationRuleMapperMock= Mockito.mock(Initiative2RewardNotificationRuleMapper.class);
        RewardNotificationRuleService rewardNotificationRuleServiceMock = Mockito.mock(RewardNotificationRuleService.class);
        ErrorNotifierService errorNotifierServiceMock = Mockito.mock(ErrorNotifierService.class);
        RewardNotificationRuleValidatorService rewardNotificationRuleValidatorServiceMock = Mockito.mock(RewardNotificationRuleValidatorService.class);

        RefundRuleMediatorService mediator = new RefundRuleMediatorServiceImpl("appName", 1000,initiative2RewardNotificationRuleMapperMock,rewardNotificationRuleValidatorServiceMock, rewardNotificationRuleServiceMock,errorNotifierServiceMock, TestUtils.objectMapper);


        RewardNotificationRule rewardNotificationRule1 = RewardNotificationRuleFaker.mockInstance(1);
        RewardNotificationRule rewardNotificationRule2 = RewardNotificationRuleFaker.mockInstance(2);

        Flux<Message<String>> msgs = Flux.just(rewardNotificationRule1, rewardNotificationRule2)
                .map(TestUtils::jsonSerializer)
                .map(payload -> MessageBuilder
                        .withPayload(payload)
                        .setHeader(KafkaHeaders.RECEIVED_PARTITION_ID, 0)
                        .setHeader(KafkaHeaders.OFFSET, 0L)
                )
                .doOnNext(m->m.setHeader(ErrorNotifierServiceImpl.ERROR_MSG_HEADER_APPLICATION_NAME, "otherAppName".getBytes(StandardCharsets.UTF_8)))
                .map(MessageBuilder::build);

        // When
        mediator.execute(msgs);

        // Then
        Mockito.verifyNoInteractions(initiative2RewardNotificationRuleMapperMock, rewardNotificationRuleServiceMock, errorNotifierServiceMock);
    }
}