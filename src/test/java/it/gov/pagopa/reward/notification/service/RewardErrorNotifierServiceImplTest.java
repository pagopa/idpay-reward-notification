package it.gov.pagopa.reward.notification.service;

import it.gov.pagopa.common.kafka.service.ErrorNotifierService;
import it.gov.pagopa.reward.notification.config.KafkaConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class RewardErrorNotifierServiceImplTest {

    private RewardErrorNotifierServiceImpl errorNotifierService;
    private static final String DUMMY_MESSAGE="DUMMY MESSAGE";
    private static final Message<String> dummyMessage = MessageBuilder.withPayload(DUMMY_MESSAGE).build();
    @Mock
    private ErrorNotifierService mockedErrorNotifierService;
    @Mock
    private KafkaConfiguration mockedKafkaConfigurationMock;

    private static final String REWARD_RULE_CONSUMER_IN_0 = "refundRuleConsumer-in-0";
    private static final  String REWARD_TRX_CONSUMER_IN_0 = "rewardTrxConsumer-in-0";
    private static final  String IBAN_OUTCOME_CONSUMER_IN_0 = "ibanOutcomeConsumer-in-0";
    private static final String REWARD_NOTIFICATION_UPLOAD_CONSUMER_IN_0 = "rewardNotificationUploadConsumer-in-0";
    private static final String COMMANDS_CONSUMER_IN_0 = "commandsConsumer-in-0";
    private static final String RULE_TYPE = "refundRuleType";
    private static final String RULE_SERVER = "refundRuleServer";
    private static final String RULE_TOPIC = "refundRuleTopic";
    private static final String RULE_GROUP = "refundRuleGroup";

    private ArgumentCaptor<KafkaConfiguration.BaseKafkaInfoDTO> baseKafkaInfoDTOArgumentCaptor;
    @BeforeEach
    void setUp() {
        errorNotifierService = new RewardErrorNotifierServiceImpl(
                mockedErrorNotifierService,
                mockedKafkaConfigurationMock
        );
        baseKafkaInfoDTOArgumentCaptor = ArgumentCaptor.forClass(KafkaConfiguration.BaseKafkaInfoDTO.class);
    }

    @Test
    void notifyRewardNotifierRule_ShouldCallErrorNotifierService() {
        Mockito.when(mockedKafkaConfigurationMock.getStream()).thenReturn(Mockito.mock(KafkaConfiguration.Stream.class));
        Mockito.when(mockedKafkaConfigurationMock.getStream().getBindings()).thenReturn(
                Map.of(
                        REWARD_RULE_CONSUMER_IN_0,
                        KafkaConfiguration.KafkaInfoDTO.builder()
                                .group(RULE_GROUP)
                                .type(RULE_TYPE)
                                .brokers(RULE_SERVER)
                                .destination(RULE_TOPIC)
                                .build()
                )
        );

        errorNotifyMock(baseKafkaInfoDTOArgumentCaptor, true, true );

        errorNotifierService.notifyRewardNotifierRule(dummyMessage, DUMMY_MESSAGE, true, new Throwable(DUMMY_MESSAGE));

        KafkaConfiguration.BaseKafkaInfoDTO capturedSrcDetails = baseKafkaInfoDTOArgumentCaptor.getValue();

        Assertions.assertEquals(RULE_TYPE, capturedSrcDetails.getType());
        Assertions.assertEquals(RULE_TOPIC, capturedSrcDetails.getDestination());
        Assertions.assertEquals(RULE_SERVER, capturedSrcDetails.getBrokers());
        Assertions.assertEquals(RULE_GROUP, capturedSrcDetails.getGroup());

        Mockito.verifyNoMoreInteractions(mockedErrorNotifierService);
    }
    @Test
    void notifyRewardResponse_ShouldCallErrorNotifierService() {
        Mockito.when(mockedKafkaConfigurationMock.getStream()).thenReturn(Mockito.mock(KafkaConfiguration.Stream.class));
        Mockito.when(mockedKafkaConfigurationMock.getStream().getBindings()).thenReturn(
                Map.of(
                        REWARD_TRX_CONSUMER_IN_0,
                        KafkaConfiguration.KafkaInfoDTO.builder()
                                .group(RULE_GROUP)
                                .type(RULE_TYPE)
                                .brokers(RULE_SERVER)
                                .destination(RULE_TOPIC)
                                .build()
                )
        );

        errorNotifyMock(baseKafkaInfoDTOArgumentCaptor, true, true );

        errorNotifierService.notifyRewardResponse(dummyMessage, DUMMY_MESSAGE, true, new Throwable(DUMMY_MESSAGE));

        KafkaConfiguration.BaseKafkaInfoDTO capturedSrcDetails = baseKafkaInfoDTOArgumentCaptor.getValue();

        Assertions.assertEquals(RULE_TYPE, capturedSrcDetails.getType());
        Assertions.assertEquals(RULE_TOPIC, capturedSrcDetails.getDestination());
        Assertions.assertEquals(RULE_SERVER, capturedSrcDetails.getBrokers());
        Assertions.assertEquals(RULE_GROUP, capturedSrcDetails.getGroup());

        Mockito.verifyNoMoreInteractions(mockedErrorNotifierService);
    }
    @Test
    void notifyRewardIbanOutcome_ShouldCallErrorNotifierService() {
        Mockito.when(mockedKafkaConfigurationMock.getStream()).thenReturn(Mockito.mock(KafkaConfiguration.Stream.class));
        Mockito.when(mockedKafkaConfigurationMock.getStream().getBindings()).thenReturn(
                Map.of(
                        IBAN_OUTCOME_CONSUMER_IN_0,
                        KafkaConfiguration.KafkaInfoDTO.builder()
                                .group(RULE_GROUP)
                                .type(RULE_TYPE)
                                .brokers(RULE_SERVER)
                                .destination(RULE_TOPIC)
                                .build()
                )
        );

        errorNotifyMock(baseKafkaInfoDTOArgumentCaptor, true, true );

        errorNotifierService.notifyRewardIbanOutcome(dummyMessage, DUMMY_MESSAGE, true, new Throwable(DUMMY_MESSAGE));

        KafkaConfiguration.BaseKafkaInfoDTO capturedSrcDetails = baseKafkaInfoDTOArgumentCaptor.getValue();

        Assertions.assertEquals(RULE_TYPE, capturedSrcDetails.getType());
        Assertions.assertEquals(RULE_TOPIC, capturedSrcDetails.getDestination());
        Assertions.assertEquals(RULE_SERVER, capturedSrcDetails.getBrokers());
        Assertions.assertEquals(RULE_GROUP, capturedSrcDetails.getGroup());

        Mockito.verifyNoMoreInteractions(mockedErrorNotifierService);
    }
    @Test
    void notifyRewardCommands_ShouldCallErrorNotifierService() {
        Mockito.when(mockedKafkaConfigurationMock.getStream()).thenReturn(Mockito.mock(KafkaConfiguration.Stream.class));
        Mockito.when(mockedKafkaConfigurationMock.getStream().getBindings()).thenReturn(
                Map.of(
                        COMMANDS_CONSUMER_IN_0,
                        KafkaConfiguration.KafkaInfoDTO.builder()
                                .group(RULE_GROUP)
                                .type(RULE_TYPE)
                                .brokers(RULE_SERVER)
                                .destination(RULE_TOPIC)
                                .build()
                )
        );

        errorNotifyMock(baseKafkaInfoDTOArgumentCaptor, true, true );

        errorNotifierService.notifyRewardCommands(dummyMessage, DUMMY_MESSAGE, true, new Throwable(DUMMY_MESSAGE));

        KafkaConfiguration.BaseKafkaInfoDTO capturedSrcDetails = baseKafkaInfoDTOArgumentCaptor.getValue();

        Assertions.assertEquals(RULE_TYPE, capturedSrcDetails.getType());
        Assertions.assertEquals(RULE_TOPIC, capturedSrcDetails.getDestination());
        Assertions.assertEquals(RULE_SERVER, capturedSrcDetails.getBrokers());
        Assertions.assertEquals(RULE_GROUP, capturedSrcDetails.getGroup());

        Mockito.verifyNoMoreInteractions(mockedErrorNotifierService);
    }
    @Test
    void notifyOrganizationFeedbackUpload_ShouldCallErrorNotifierService() {
        Mockito.when(mockedKafkaConfigurationMock.getStream()).thenReturn(Mockito.mock(KafkaConfiguration.Stream.class));
        Mockito.when(mockedKafkaConfigurationMock.getStream().getBindings()).thenReturn(
                Map.of(
                        REWARD_NOTIFICATION_UPLOAD_CONSUMER_IN_0,
                        KafkaConfiguration.KafkaInfoDTO.builder()
                                .group(RULE_GROUP)
                                .type(RULE_TYPE)
                                .brokers(RULE_SERVER)
                                .destination(RULE_TOPIC)
                                .build()
                )
        );

        errorNotifyMock(baseKafkaInfoDTOArgumentCaptor, true, true );

        errorNotifierService.notifyOrganizationFeedbackUpload(dummyMessage, DUMMY_MESSAGE, true, new Throwable(DUMMY_MESSAGE));

        KafkaConfiguration.BaseKafkaInfoDTO capturedSrcDetails = baseKafkaInfoDTOArgumentCaptor.getValue();

        Assertions.assertEquals(RULE_TYPE, capturedSrcDetails.getType());
        Assertions.assertEquals(RULE_TOPIC, capturedSrcDetails.getDestination());
        Assertions.assertEquals(RULE_SERVER, capturedSrcDetails.getBrokers());
        Assertions.assertEquals(RULE_GROUP, capturedSrcDetails.getGroup());

        Mockito.verifyNoMoreInteractions(mockedErrorNotifierService);

    }

    private void errorNotifyMock(ArgumentCaptor<KafkaConfiguration.BaseKafkaInfoDTO> baseKafkaInfoDTO, boolean retryable, boolean resendApplication ) {
        Mockito.when(mockedErrorNotifierService.notify(
                        baseKafkaInfoDTO.capture(),
                        eq(DUMMY_MESSAGE),
                        eq(retryable),
                        any(),
                        eq(resendApplication),
                        eq(dummyMessage)
                )
        ).thenReturn(true);
    }
}