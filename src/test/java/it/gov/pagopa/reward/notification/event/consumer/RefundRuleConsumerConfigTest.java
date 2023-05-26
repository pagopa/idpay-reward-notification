package it.gov.pagopa.reward.notification.event.consumer;

import it.gov.pagopa.common.kafka.utils.KafkaConstants;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.dto.rule.AccumulatedAmountDTO;
import it.gov.pagopa.reward.notification.dto.rule.TimeParameterDTO;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.repository.RewardNotificationRuleRepository;
import it.gov.pagopa.reward.notification.test.fakers.InitiativeRefundDTOFaker;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.reward.notification.service.rule.RewardNotificationRuleServiceImpl=WARN",
        "logging.level.it.gov.pagopa.common.reactive.utils.PerformanceLogger=WARN",
})
public class RefundRuleConsumerConfigTest extends BaseIntegrationTest {

    @Autowired
    private RewardNotificationRuleRepository rewardNotificationRuleRepository;

    @AfterEach
    void cleanData(){
        rewardNotificationRuleRepository.deleteAll().block();
    }

    @Test
    void testRewardNotificationRulePersistence(){
        int validInitiatives = 100;
        int notValidInitiatives = errorUseCases.size();
        long maxWaitingMs = 30000;

        List<String> initiativePayloads = new ArrayList<>();
        initiativePayloads.addAll(buildValidPayloads(notValidInitiatives, validInitiatives/2));
        initiativePayloads.addAll(IntStream.range(0,notValidInitiatives).mapToObj(i -> errorUseCases.get(i).getFirst().get()).toList());
        initiativePayloads.addAll(buildValidPayloads(notValidInitiatives + (validInitiatives / 2) + notValidInitiatives, validInitiatives / 2));

        long timeStart=System.currentTimeMillis();
        initiativePayloads.forEach(i->kafkaTestUtilitiesService.publishIntoEmbeddedKafka(topicInitiative2StoreConsumer, null, null, i));
        kafkaTestUtilitiesService.publishIntoEmbeddedKafka(topicInitiative2StoreConsumer, List.of(new RecordHeader(KafkaConstants.ERROR_MSG_HEADER_APPLICATION_NAME, "OTHERAPPNAME".getBytes(StandardCharsets.UTF_8))), null, "OTHERAPPMESSAGE");
        long timePublishingEnd=System.currentTimeMillis();

        long countSaved = waitForInitiativeStored(validInitiatives);
        long timeEnd=System.currentTimeMillis();

        Assertions.assertEquals(validInitiatives, countSaved);

        checkErrorsPublished(notValidInitiatives, maxWaitingMs, errorUseCases);

        System.out.printf("""
            ************************
            Time spent to send %d (%d + %d) messages (from start): %d millis
            Time spent to assert reward notification rules stored count (from previous check): %d millis
            ************************
            Test Completed in %d millis
            ************************
            """,
                validInitiatives + notValidInitiatives,
                validInitiatives,
                notValidInitiatives,
                timePublishingEnd-timeStart,
                timeEnd-timePublishingEnd,
                timeEnd-timeStart
        );

        long timeCommitCheckStart = System.currentTimeMillis();
        Map<TopicPartition, OffsetAndMetadata> srcCommitOffsets = kafkaTestUtilitiesService.checkCommittedOffsets(topicInitiative2StoreConsumer, groupIdInitiative2StoreConsumer, initiativePayloads.size()+1); // +1 due to other applicationName useCase
        long timeCommitCheckEnd = System.currentTimeMillis();

        System.out.printf("""
                        ************************
                        Time occurred to check committed offset: %d millis
                        ************************
                        Source Topic Committed Offsets: %s
                        ************************
                        """,
                timeCommitCheckEnd - timeCommitCheckStart,
                srcCommitOffsets
        );
    }

    private long waitForInitiativeStored(int n) {
        return waitForInitiativeStored(n, rewardNotificationRuleRepository);
    }

    public static long waitForInitiativeStored(int n, RewardNotificationRuleRepository rewardNotificationRuleRepository) {
        long[] countSaved={0};
        //noinspection ConstantConditions
        TestUtils.waitFor(()->(countSaved[0]=rewardNotificationRuleRepository.count().block()) >= n, ()->"Expected %d saved reward notification rules, read %d".formatted(n, countSaved[0]), 60, 1000);
        return countSaved[0];
    }

    private List<String> buildValidPayloads(int bias, int n) {
        return IntStream.range(bias, bias+n)
                .mapToObj(InitiativeRefundDTOFaker::mockInstance)
                .map(TestUtils::jsonSerializer)
                .toList();
    }

    private final List<Pair<Supplier<String>, Consumer<ConsumerRecord<String,String>>>> errorUseCases = new ArrayList<>();
    {
        String useCaseJsonNotExpected = "{\"initiativeId\":\"id_0\",unexpectedStructure:0}";
        errorUseCases.add(Pair.of(
                () -> useCaseJsonNotExpected,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD_NOTIFICATION_RULE] Unexpected JSON", useCaseJsonNotExpected)
        ));

        String jsonNotValid = "{\"initiativeId\":\"id_1\",invalidJson";
        errorUseCases.add(Pair.of(
                () -> jsonNotValid,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD_NOTIFICATION_RULE] Unexpected JSON", jsonNotValid)
        ));

        RewardNotificationRule notValidRule = new RewardNotificationRule();
        notValidRule.setInitiativeId("id_2");
        String notValidRuleJson = TestUtils.jsonSerializer(notValidRule);
        errorUseCases.add(Pair.of(
                () -> notValidRuleJson,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD_NOTIFICATION_RULE] An error occurred handling initiative", notValidRuleJson)
        ));

        RewardNotificationRule notValidTimeRule = new RewardNotificationRule();
        notValidTimeRule.setInitiativeId("id_3");
        notValidTimeRule.setTimeParameter(new TimeParameterDTO());
        String notValidTimeRuleJson = TestUtils.jsonSerializer(notValidTimeRule);
        errorUseCases.add(Pair.of(
                () -> notValidTimeRuleJson,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD_NOTIFICATION_RULE] An error occurred handling initiative", notValidTimeRuleJson)
        ));

        RewardNotificationRule notValidTimeClosedRule = new RewardNotificationRule();
        notValidTimeClosedRule.setInitiativeId("id_4");
        notValidTimeClosedRule.setTimeParameter(new TimeParameterDTO());
        notValidTimeClosedRule.getTimeParameter().setTimeType(TimeParameterDTO.TimeTypeEnum.CLOSED);
        String notValidTimeClosedRuleJson = TestUtils.jsonSerializer(notValidTimeClosedRule);
        errorUseCases.add(Pair.of(
                () -> notValidTimeClosedRuleJson,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD_NOTIFICATION_RULE] An error occurred handling initiative", notValidTimeClosedRuleJson)
        ));

        RewardNotificationRule notValidAccumulatedRule = new RewardNotificationRule();
        notValidAccumulatedRule.setInitiativeId("id_5");
        notValidAccumulatedRule.setAccumulatedAmount(new AccumulatedAmountDTO());
        String notValidAccumulatedRuleJson = TestUtils.jsonSerializer(notValidAccumulatedRule);
        errorUseCases.add(Pair.of(
                () -> notValidAccumulatedRuleJson,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD_NOTIFICATION_RULE] An error occurred handling initiative", notValidAccumulatedRuleJson)
        ));

        RewardNotificationRule notValidThresholdRule = new RewardNotificationRule();
        notValidThresholdRule.setInitiativeId("id_6");
        notValidThresholdRule.setAccumulatedAmount(new AccumulatedAmountDTO());
        notValidThresholdRule.getAccumulatedAmount().setAccumulatedType(AccumulatedAmountDTO.AccumulatedTypeEnum.THRESHOLD_REACHED);
        String notValidThresholdRuleJson = TestUtils.jsonSerializer(notValidThresholdRule);
        errorUseCases.add(Pair.of(
                () -> notValidThresholdRuleJson,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD_NOTIFICATION_RULE] An error occurred handling initiative", notValidThresholdRuleJson)
        ));
    }

    private void checkErrorMessageHeaders(ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload) {
        checkErrorMessageHeaders(topicInitiative2StoreConsumer, groupIdInitiative2StoreConsumer, errorMessage, errorDescription, expectedPayload,null);
    }
}