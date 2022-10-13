package it.gov.pagopa.reward.notification.service.iban.request;

import com.fasterxml.jackson.core.JsonParseException;
import it.gov.pagopa.reward.notification.dto.iban.IbanRequestDTO;
import it.gov.pagopa.reward.notification.dto.mapper.IbanRequestDTO2RewardIbanMapper;
import it.gov.pagopa.reward.notification.model.RewardIban;
import it.gov.pagopa.reward.notification.service.ErrorNotifierService;
import it.gov.pagopa.reward.notification.service.iban.RewardIbanService;
import it.gov.pagopa.reward.notification.test.fakers.IbanRequestDTOFaker;
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
class IbanRequestMediatorServiceImplTest {

    @Mock
    private IbanRequestDTO2RewardIbanMapper ibanRequestDTO2RewardIbanMapperMock;

    @Mock
    private RewardIbanService rewardIbanServiceMock;

    @Mock
    private ErrorNotifierService errorNotifierServiceMock;

    private IbanRequestMediatorService ibanRequestMediatorService;

    @BeforeEach
    void setUp() {
        ibanRequestMediatorService = new IbanRequestMediatorServiceImpl(1000, ibanRequestDTO2RewardIbanMapperMock, rewardIbanServiceMock, errorNotifierServiceMock, TestUtils.objectMapper);
    }

    @Test
    void execute(){
        // Given
        IbanRequestDTO ibanRequestDTO1 = IbanRequestDTOFaker.mockInstance(1);
        Message<String> msg1 = MessageBuilder.withPayload(TestUtils.jsonSerializer(ibanRequestDTO1)).build();
        IbanRequestDTO ibanRequestDTO2 = IbanRequestDTOFaker.mockInstance(2);
        Message<String> msg2 = MessageBuilder.withPayload(TestUtils.jsonSerializer(ibanRequestDTO2)).build();
        Message<String> msg3 = MessageBuilder.withPayload("INVALID JSON").build();

        Flux<Message<String>> messageFlux = Flux.just(msg1, msg2, msg3);

        RewardIban rewardIban1 = RewardIban.builder()
                .id(ibanRequestDTO1.getUserId().concat(ibanRequestDTO1.getInitiativeId()))
                .userId(ibanRequestDTO1.getUserId())
                .initiativeId(ibanRequestDTO1.getInitiativeId())
                .iban(ibanRequestDTO1.getIban())
                .timestamp(LocalDateTime.now()).build();
        Mockito.when(ibanRequestDTO2RewardIbanMapperMock.apply(ibanRequestDTO1)).thenReturn(rewardIban1);
        Mockito.when(ibanRequestDTO2RewardIbanMapperMock.apply(ibanRequestDTO2)).thenThrow(RuntimeException.class);

        Mockito.when(rewardIbanServiceMock.save(Mockito.same(rewardIban1))).thenAnswer(i -> Mono.just(i.getArguments()[0]));

        // When
        ibanRequestMediatorService.execute(messageFlux);

        // Then
        Mockito.verify(ibanRequestDTO2RewardIbanMapperMock, Mockito.times(2)).apply(Mockito.any());
        Mockito.verify(rewardIbanServiceMock).save(Mockito.any());
        Mockito.verify(errorNotifierServiceMock, Mockito.times(2)).notifyRewardIbanRequest(Mockito.any(Message.class), Mockito.anyString(), Mockito.same(false),Mockito.any(Throwable.class));
        Mockito.verify(errorNotifierServiceMock).notifyRewardIbanRequest(Mockito.any(Message.class), Mockito.anyString(), Mockito.same(false),Mockito.any(JsonParseException.class));
    }
}