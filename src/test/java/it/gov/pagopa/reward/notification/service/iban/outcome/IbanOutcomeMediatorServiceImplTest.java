package it.gov.pagopa.reward.notification.service.iban.outcome;

import com.fasterxml.jackson.core.JsonParseException;
import it.gov.pagopa.reward.notification.dto.iban.IbanOutcomeDTO;
import it.gov.pagopa.reward.notification.model.RewardIban;
import it.gov.pagopa.reward.notification.service.ErrorNotifierService;
import it.gov.pagopa.reward.notification.service.ErrorNotifierServiceImpl;
import it.gov.pagopa.reward.notification.utils.IbanConstants;
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

import java.nio.charset.StandardCharsets;
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
                    "appName",
                    1000,
                    ibanOutcomeOperationsServiceMock,
                    errorNotifierServiceMock,
                    TestUtils.objectMapper);
    }

    @Test
    void execute(){
        // Given
        //Initializing KO messages
        IbanOutcomeDTO ibanOutcomeKoDTO = IbanOutcomeDTOFaker.mockInstance(1);
        ibanOutcomeKoDTO.setStatus(IbanConstants.STATUS_KO);
        Message<String> msgKo = MessageBuilder.withPayload(TestUtils.jsonSerializer(ibanOutcomeKoDTO)).build();
        IbanOutcomeDTO ibanOutcomeKoExceptionDTO = IbanOutcomeDTOFaker.mockInstance(2);
        ibanOutcomeKoExceptionDTO.setStatus(IbanConstants.STATUS_KO);
        Message<String> msgKoException = MessageBuilder.withPayload(TestUtils.jsonSerializer(ibanOutcomeKoExceptionDTO)).build();

        //Initializing UNKNOWN_PSP message
        IbanOutcomeDTO ibanOutcomeUnknownPspDTO = IbanOutcomeDTOFaker.mockInstance(3);
        ibanOutcomeUnknownPspDTO.setStatus(IbanConstants.STATUS_UNKNOWN_PSP);
        Message<String> msgUnknownPsp = MessageBuilder.withPayload(TestUtils.jsonSerializer(ibanOutcomeUnknownPspDTO)).build();

        //Initializing Ok message
        IbanOutcomeDTO ibanOutcomeOkDTO = IbanOutcomeDTOFaker.mockInstance(4);
        ibanOutcomeOkDTO.setStatus(IbanConstants.STATUS_OK);
        Message<String> msgOk = MessageBuilder.withPayload(TestUtils.jsonSerializer(ibanOutcomeOkDTO)).build();

        //Initializing invalid message
        Message<String> msgJsonInvalid = MessageBuilder.withPayload("INVALID JSON").build();

        Flux<Message<String>> messageFlux = Flux.just(msgKo, msgKoException, msgUnknownPsp, msgOk, msgJsonInvalid);

        RewardIban rewardIbanKo = RewardIban.builder()
                .id(ibanOutcomeKoDTO.getUserId().concat(ibanOutcomeKoDTO.getInitiativeId()))
                .userId(ibanOutcomeKoDTO.getUserId())
                .initiativeId(ibanOutcomeKoDTO.getInitiativeId())
                .iban(ibanOutcomeKoDTO.getIban())
                .checkIbanOutcome(ibanOutcomeKoDTO.getStatus())
                .timestamp(LocalDateTime.now()).build();

        RewardIban rewardIbanUnknownPsp = RewardIban.builder()
                .id(ibanOutcomeUnknownPspDTO.getUserId().concat(ibanOutcomeUnknownPspDTO.getInitiativeId()))
                .userId(ibanOutcomeUnknownPspDTO.getUserId())
                .initiativeId(ibanOutcomeUnknownPspDTO.getInitiativeId())
                .iban(ibanOutcomeUnknownPspDTO.getIban())
                .checkIbanOutcome(ibanOutcomeUnknownPspDTO.getStatus())
                .timestamp(LocalDateTime.now()).build();

        RewardIban rewardIbanOk = RewardIban.builder()
                .id(ibanOutcomeOkDTO.getUserId().concat(ibanOutcomeOkDTO.getInitiativeId()))
                .userId(ibanOutcomeOkDTO.getUserId())
                .initiativeId(ibanOutcomeOkDTO.getInitiativeId())
                .iban(ibanOutcomeOkDTO.getIban())
                .checkIbanOutcome(ibanOutcomeOkDTO.getStatus())
                .timestamp(LocalDateTime.now()).build();

        Mockito.when(ibanOutcomeOperationsServiceMock.execute(ibanOutcomeKoDTO)).thenReturn(Mono.just(rewardIbanKo));
        Mockito.when(ibanOutcomeOperationsServiceMock.execute(ibanOutcomeKoExceptionDTO)).thenThrow(RuntimeException.class);
        Mockito.when(ibanOutcomeOperationsServiceMock.execute(ibanOutcomeUnknownPspDTO)).thenReturn(Mono.just(rewardIbanUnknownPsp));
        Mockito.when(ibanOutcomeOperationsServiceMock.execute(ibanOutcomeOkDTO)).thenReturn(Mono.just(rewardIbanOk));

        // When
        ibanOutcomeMediatorService.execute(messageFlux);

        // Then
        Mockito.verify(ibanOutcomeOperationsServiceMock, Mockito.times(4)).execute(Mockito.any());
        Mockito.verify(errorNotifierServiceMock, Mockito.times(2)).notifyRewardIbanOutcome(Mockito.any(Message.class), Mockito.anyString(), Mockito.same(false),Mockito.any(Throwable.class));
        Mockito.verify(errorNotifierServiceMock).notifyRewardIbanOutcome(Mockito.any(Message.class), Mockito.anyString(), Mockito.same(false),Mockito.any(JsonParseException.class));
    }

    @Test
    void otherApplicationRetryTest(){
        // Given
        IbanOutcomeDTO ibanOutcomeDTO1 = IbanOutcomeDTOFaker.mockInstance(1);
        IbanOutcomeDTO ibanOutcomeDTO2 = IbanOutcomeDTOFaker.mockInstance(2);

        Flux<Message<String>> msgs = Flux.just(ibanOutcomeDTO1, ibanOutcomeDTO2)
                .map(TestUtils::jsonSerializer)
                .map(MessageBuilder::withPayload)
                .doOnNext(m->m.setHeader(ErrorNotifierServiceImpl.ERROR_MSG_HEADER_APPLICATION_NAME, "otherAppName".getBytes(StandardCharsets.UTF_8)))
                .map(MessageBuilder::build);

        // When
        ibanOutcomeMediatorService.execute(msgs);

        // Then
        Mockito.verifyNoInteractions(ibanOutcomeOperationsServiceMock, errorNotifierServiceMock);
    }
}