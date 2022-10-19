package it.gov.pagopa.reward.notification.service.iban.outcome;

import com.fasterxml.jackson.core.JsonParseException;
import it.gov.pagopa.reward.notification.dto.iban.IbanOutcomeDTO;
import it.gov.pagopa.reward.notification.model.RewardIban;
import it.gov.pagopa.reward.notification.service.ErrorNotifierService;
import it.gov.pagopa.reward.notification.service.utils.IbanConstants;
import it.gov.pagopa.reward.notification.test.fakers.IbanOutcomeDTOFaker;
import it.gov.pagopa.reward.notification.test.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
class IbanOutcomeMediatorServiceImplTest {
    @Mock
    private IbanOutcomeOperationsService ibanOutcomeOperationsServiceMock;
    @Mock
    private ErrorNotifierService errorNotifierServiceMock;


    private IbanOutcomeMediatorService ibanOutcomeMediatorService;

    @BeforeEach
    void setUp() {
            ibanOutcomeMediatorService = new IbanOutcomeMediatorServiceImpl(
                    1000,
                    ibanOutcomeOperationsServiceMock,
                    errorNotifierServiceMock,
                    TestUtils.objectMapper);
    }

    @Test
    void execute(){
        // Given
        //Initializing KO messages
        IbanOutcomeDTO ibanOutcomeDTO1 = IbanOutcomeDTOFaker.mockInstance(1);
        ibanOutcomeDTO1.setStatus(IbanConstants.STATUS_KO);
        Message<String> msg1 = MessageBuilder.withPayload(TestUtils.jsonSerializer(ibanOutcomeDTO1)).build();
        IbanOutcomeDTO ibanOutcomeDTO2 = IbanOutcomeDTOFaker.mockInstance(2);
        ibanOutcomeDTO2.setStatus(IbanConstants.STATUS_KO);
        Message<String> msg2 = MessageBuilder.withPayload(TestUtils.jsonSerializer(ibanOutcomeDTO2)).build();

        //Initializing UNKNOWN_PSP message
        IbanOutcomeDTO ibanOutcomeDTO3 = IbanOutcomeDTOFaker.mockInstance(2);
        ibanOutcomeDTO3.setStatus(IbanConstants.STATUS_UNKNOWN_PSP);
        Message<String> msg3 = MessageBuilder.withPayload(TestUtils.jsonSerializer(ibanOutcomeDTO3)).build();

        //Initializing invalid message
        Message<String> msg4 = MessageBuilder.withPayload("INVALID JSON").build();

        Flux<Message<String>> messageFlux = Flux.just(msg1, msg2, msg3, msg4);

        RewardIban rewardIban1 = RewardIban.builder()
                .id(ibanOutcomeDTO1.getUserId().concat(ibanOutcomeDTO1.getInitiativeId()))
                .userId(ibanOutcomeDTO1.getUserId())
                .initiativeId(ibanOutcomeDTO1.getInitiativeId())
                .iban(ibanOutcomeDTO1.getIban())
                .checkIbanOutcome(ibanOutcomeDTO1.getStatus())
                .timestamp(LocalDateTime.now()).build();

        RewardIban rewardIban3 = RewardIban.builder()
                .id(ibanOutcomeDTO3.getUserId().concat(ibanOutcomeDTO3.getInitiativeId()))
                .userId(ibanOutcomeDTO3.getUserId())
                .initiativeId(ibanOutcomeDTO3.getInitiativeId())
                .iban(ibanOutcomeDTO3.getIban())
                .checkIbanOutcome(ibanOutcomeDTO3.getStatus())
                .timestamp(LocalDateTime.now()).build();

        Mockito.when(ibanOutcomeOperationsServiceMock.execute(ibanOutcomeDTO1)).thenReturn(Mono.just(rewardIban1));
        Mockito.when(ibanOutcomeOperationsServiceMock.execute(ibanOutcomeDTO2)).thenThrow(RuntimeException.class);
        Mockito.when(ibanOutcomeOperationsServiceMock.execute(ibanOutcomeDTO3)).thenReturn(Mono.just(rewardIban3));

        // When
        ibanOutcomeMediatorService.execute(messageFlux);

        // Then
        Mockito.verify(ibanOutcomeOperationsServiceMock, Mockito.times(3)).execute(Mockito.any());
        Mockito.verify(errorNotifierServiceMock, Mockito.times(2)).notifyRewardIbanOutcome(Mockito.any(Message.class), Mockito.anyString(), Mockito.same(false),Mockito.any(Throwable.class));
        Mockito.verify(errorNotifierServiceMock).notifyRewardIbanOutcome(Mockito.any(Message.class), Mockito.anyString(), Mockito.same(false),Mockito.any(JsonParseException.class));
    }
}