package it.gov.pagopa.reward.notification.service.commands;

import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.utils.MemoryAppender;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.notification.dto.commands.CommandOperationDTO;
import it.gov.pagopa.reward.notification.service.RewardErrorNotifierService;
import it.gov.pagopa.reward.notification.service.commands.ops.DeleteInitiativeService;
import it.gov.pagopa.reward.notification.utils.CommandsConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.LoggerContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import static org.mockito.ArgumentMatchers.anyString;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class CommandsMediatorServiceImplTest {
    @Mock
    private RewardErrorNotifierService rewardErrorNotifierServiceMock;
    @Mock
    private DeleteInitiativeService deleteInitiativeServiceMock;
    private CommandsMediatorServiceImpl commandsMediatorService;
    private MemoryAppender memoryAppender;
    @BeforeEach
    void setUp() {
        commandsMediatorService =
                new CommandsMediatorServiceImpl(
                        "Application Name",
                        100L,
                        deleteInitiativeServiceMock,
                        rewardErrorNotifierServiceMock,
                        TestUtils.objectMapper);

        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("it.gov.pagopa.reward.notification.service.commands.CommandsMediatorServiceImpl");
        memoryAppender = new MemoryAppender();
        memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(ch.qos.logback.classic.Level.INFO);
        logger.addAppender(memoryAppender);
        memoryAppender.start();
    }
    @Test
    void getCommitDelay() {
        //given
        Duration expected = Duration.ofMillis(100L);
        //when
        Duration commitDelay = commandsMediatorService.getCommitDelay();
        //then
        Assertions.assertEquals(expected,commitDelay);
    }
    @Test
    void givenMessagesWhenAfterCommitsThenSuccessfully() {
        //given
        Flux<List<String>> afterCommits2Subscribe = Flux.just(List.of("INITIATIVE1", "INITIATIVE2", "INITIATIVE3"));

        // when
        commandsMediatorService.subscribeAfterCommits(afterCommits2Subscribe);

        //then
        Assertions.assertEquals(
                ("[REWARD_NOTIFICATION_COMMANDS] Processed offsets committed successfully"),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }

    @Test
    void getObjectReader() {
        ObjectReader objectReader = commandsMediatorService.getObjectReader();
        Assertions.assertNotNull(objectReader);
    }

    @Test
    void givenDeleteInitiativeOperationTypeWhenCallExecuteThenReturnString() {
        //given
        CommandOperationDTO payload = CommandOperationDTO.builder()
                .entityId("DUMMY_INITIATIVEID")
                .operationTime(LocalDateTime.now())
                .operationType(CommandsConstants.COMMANDS_OPERATION_TYPE_DELETE_INITIATIVE)
                .build();

        Message<String> message = MessageBuilder.withPayload("INITIATIVE").setHeader("HEADER", "DUMMY_HEADER").build();
        Map<String, Object> ctx = new HashMap<>();

        Mockito.when(deleteInitiativeServiceMock.execute(payload.getEntityId())).thenReturn(Mono.just(anyString()));

        //when
        String result = commandsMediatorService.execute(payload, message, ctx).block();

        //then
        Assertions.assertNotNull(result);
        Mockito.verify(deleteInitiativeServiceMock).execute(anyString());
    }

    @Test
    void givenOperationTypeDifferentWhenCallExecuteThenReturnMonoEmpty() {
        //given
        CommandOperationDTO payload = CommandOperationDTO.builder()
                .entityId("DUMMY_INITIATIVEID")
                .operationTime(LocalDateTime.now())
                .operationType("OTHER_OPERATION_TYPE")
                .build();

        Message<String> message = MessageBuilder.withPayload("INITIATIVE").setHeader("HEADER", "DUMMY_HEADER").build();
        Map<String, Object> ctx = new HashMap<>();
        //when
        Mono<String> result = commandsMediatorService.execute(payload, message, ctx);

        //then
        Assertions.assertEquals(Mono.empty(), result);
        Mockito.verify(deleteInitiativeServiceMock, Mockito.never()).execute(anyString());
    }

    @Test
    void getFlowName() {
        //given
        String expected = "REWARD_NOTIFICATION_COMMANDS";
        //when
        String result = commandsMediatorService.getFlowName();
        //then
        Assertions.assertEquals(expected, result);
    }

}