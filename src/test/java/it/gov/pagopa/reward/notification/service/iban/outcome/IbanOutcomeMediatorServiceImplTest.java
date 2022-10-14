package it.gov.pagopa.reward.notification.service.iban.outcome;

import com.fasterxml.jackson.core.JsonParseException;
import it.gov.pagopa.reward.notification.dto.iban.IbanOutcomeDTO;
import it.gov.pagopa.reward.notification.dto.mapper.IbanOutcomeDTO2RewardIbanMapper;
import it.gov.pagopa.reward.notification.model.RewardIban;
import it.gov.pagopa.reward.notification.service.ErrorNotifierService;
import it.gov.pagopa.reward.notification.service.iban.RewardIbanService;
import it.gov.pagopa.reward.notification.service.iban.outcome.filter.IbanOutcomeFilter;
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
    private IbanOutcomeDTO2RewardIbanMapper ibanOutcomeDTO2RewardIbanMapperMock;

    @Mock
    private IbanOutcomeFilter ibanOutcomeFilter;

    @Mock
    private RewardIbanService rewardIbanServiceMock;

    @Mock
    private ErrorNotifierService errorNotifierServiceMock;

    private IbanOutcomeMediatorService ibanOutcomeMediatorService;

    @BeforeEach
    void setUp() {
            ibanOutcomeMediatorService = new IbanOutcomeMediatorServiceImpl(1000, ibanOutcomeDTO2RewardIbanMapperMock, ibanOutcomeFilter, rewardIbanServiceMock, errorNotifierServiceMock, TestUtils.objectMapper);
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

        Mockito.when(ibanOutcomeFilter.test(ibanOutcomeDTO1)).thenReturn(true);
        Mockito.when(ibanOutcomeFilter.test(ibanOutcomeDTO2)).thenReturn(true);
        Mockito.when(ibanOutcomeFilter.test(ibanOutcomeDTO3)).thenReturn(false);

        Mockito.when(ibanOutcomeDTO2RewardIbanMapperMock.apply(ibanOutcomeDTO1)).thenReturn(rewardIban1);
        Mockito.when(ibanOutcomeDTO2RewardIbanMapperMock.apply(ibanOutcomeDTO2)).thenThrow(RuntimeException.class);

        Mockito.when(rewardIbanServiceMock.deleteIban(Mockito.same(rewardIban1))).thenAnswer(i -> Mono.just(i.getArguments()[0]));

        // When
        ibanOutcomeMediatorService.execute(messageFlux);

        // Then
        Mockito.verify(ibanOutcomeFilter, Mockito.times(3)).test(Mockito.any());
        Mockito.verify(ibanOutcomeDTO2RewardIbanMapperMock, Mockito.times(2)).apply(Mockito.any());
        Mockito.verify(rewardIbanServiceMock).deleteIban(Mockito.any());
        Mockito.verify(errorNotifierServiceMock, Mockito.times(2)).notifyRewardIbanOutcome(Mockito.any(Message.class), Mockito.anyString(), Mockito.same(false),Mockito.any(Throwable.class));
        Mockito.verify(errorNotifierServiceMock).notifyRewardIbanOutcome(Mockito.any(Message.class), Mockito.anyString(), Mockito.same(false),Mockito.any(JsonParseException.class));
    }
}