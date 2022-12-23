package it.gov.pagopa.reward.notification.service.rewards;

import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.model.Rewards;
import it.gov.pagopa.reward.notification.service.ErrorNotifierService;
import it.gov.pagopa.reward.notification.service.LockService;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.RewardNotificationRuleEvaluatorService;
import it.gov.pagopa.reward.notification.test.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ExtendWith(MockitoExtension.class)
class RewardMediatorServiceTest {

    public static final int LOCK_SERVICE_BUKET_SIZE = 1000;

    @Mock private LockService lockServiceMock;
    @Mock private RewardsService rewardsServiceMock;
    @Mock private RewardNotificationRuleEvaluatorService ruleEvaluatorServiceMock;
    @Mock private ErrorNotifierService errorNotifierServiceMock;

    private RewardsMediatorServiceImpl service;

    @BeforeEach
    void init(){
        service=new RewardsMediatorServiceImpl("APPNAME", lockServiceMock, rewardsServiceMock, ruleEvaluatorServiceMock, errorNotifierServiceMock, 500, TestUtils.objectMapper);
    }

    @Test
    void testFiltering0Rewards() {
        // Given
        Reward reward3 = new Reward(BigDecimal.ONE);
        RewardTransactionDTO trx = RewardTransactionDTO.builder()
                .rewards(Map.of(
                        "ID1", new Reward(BigDecimal.ZERO),
                        "ID2", new Reward(BigDecimal.ONE, BigDecimal.ZERO),
                        "ID3", reward3
                ))
                .build();

        Mockito.when(rewardsServiceMock.checkDuplicateReward(trx, "ID3")).thenReturn(Mono.just(trx));
        Rewards expectedResult = Rewards.builder().initiativeId("ID3").build();

        Mockito.when(ruleEvaluatorServiceMock.retrieveAndEvaluate("ID3", reward3, trx, null)).thenReturn(Mono.just(expectedResult));

        // When
        List<Rewards> result = service.execute(trx, null, new HashMap<>()).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(List.of(expectedResult),result);
    }

    @Test
    void testTrxLockIdCalculationWhenUserId() {
        Mockito.when(lockServiceMock.getBuketSize()).thenReturn(LOCK_SERVICE_BUKET_SIZE);

        final Map<Integer, Long> lockId2Count = IntStream.range(0, LOCK_SERVICE_BUKET_SIZE)
                .mapToObj(i -> service.calculateLockId(MessageBuilder.withPayload("{\"userId\":\"%s\"".formatted(UUID.nameUUIDFromBytes((i + "").getBytes(StandardCharsets.UTF_8)).toString())).build()))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        checkLockIdValues(lockId2Count);
    }

    @Test
    void testTrxLockIdCalculationWhenNoUserIdButMessageKey() {
        Mockito.when(lockServiceMock.getBuketSize()).thenReturn(LOCK_SERVICE_BUKET_SIZE);

        final Map<Integer, Long> lockId2Count = IntStream.range(0, LOCK_SERVICE_BUKET_SIZE)
                .mapToObj(i -> service.calculateLockId(MessageBuilder.withPayload("").setHeader(KafkaHeaders.RECEIVED_MESSAGE_KEY, "KEY%s".formatted(UUID.nameUUIDFromBytes((i + "").getBytes(StandardCharsets.UTF_8)).toString()).getBytes(StandardCharsets.UTF_8)).build()))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        checkLockIdValues(lockId2Count);
    }

    @Test
    void testTrxLockIdCalculationWhenNoUserIdNoMessageKeyButPartitionId() {
        Mockito.when(lockServiceMock.getBuketSize()).thenReturn(LOCK_SERVICE_BUKET_SIZE);

        final Map<Integer, Long> lockId2Count = IntStream.range(0, LOCK_SERVICE_BUKET_SIZE)
                .mapToObj(i -> service.calculateLockId(MessageBuilder.withPayload("").setHeader(KafkaHeaders.PARTITION_ID, "%d".formatted(i).getBytes(StandardCharsets.UTF_8)).build()))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        checkLockIdValues(lockId2Count);
    }

    @Test
    void testTrxLockIdCalculationWhenNoUserIdNoMessageKeyNoPartitionId() {
        Mockito.when(lockServiceMock.getBuketSize()).thenReturn(LOCK_SERVICE_BUKET_SIZE);

        final Map<Integer, Long> lockId2Count = IntStream.range(0, LOCK_SERVICE_BUKET_SIZE)
                .mapToObj(i -> service.calculateLockId(MessageBuilder.withPayload("%d".formatted(i)).build()))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        checkLockIdValues(lockId2Count);
    }

    private static void checkLockIdValues(Map<Integer, Long> lockId2Count) {
        lockId2Count.forEach((lockId, count) -> {
            Assertions.assertTrue(lockId < LOCK_SERVICE_BUKET_SIZE && lockId >= 0);
            Assertions.assertTrue(count < 10, "LockId %d hit too times: %d".formatted(lockId, count));
        });
    }
}
