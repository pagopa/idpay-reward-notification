package it.gov.pagopa.reward.notification.event.consumer;

import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.test.fakers.StorageEventDtoFaker;
import it.gov.pagopa.reward.notification.test.utils.TestUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.springframework.data.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

class OrganizationFeedbackUploadEventConsumerConfigTest extends BaseIntegrationTest {

    @Test
    void test(){
        int messages = 1;
        int notValidMessages = errorUseCases.size();

        List<String> payloads = new ArrayList<>(IntStream.range(0, messages)
                .mapToObj(StorageEventDtoFaker::mockInstance)
                .map(TestUtils::jsonSerializer)
                .map("[%s]"::formatted)
                .toList());
        payloads.addAll(IntStream.range(0,notValidMessages).mapToObj(i -> errorUseCases.get(i).getFirst().get()).toList());

        long timeStart=System.currentTimeMillis();
        payloads.forEach(p -> publishIntoEmbeddedKafka(topicRewardNotificationUpload,null,null, p));
        long timePublishingEnd=System.currentTimeMillis();

        long timeEnd = System.currentTimeMillis();

        checkErrorsPublished(notValidMessages, 5000, errorUseCases);

        System.out.printf("""
            ************************
            Time spent to send %d (%d + %d) messages (from start): %d millis
            Time spent to assert reward notification rules stored count (from previous check): %d millis
            ************************
            Test Completed in %d millis
            ************************
            """,
                messages + notValidMessages,
                messages,
                errorUseCases.size(),
                timePublishingEnd-timeStart,
                timeEnd-timePublishingEnd,
                timeEnd-timeStart
        );

        long timeCommitCheckStart = System.currentTimeMillis();
        Map<TopicPartition, OffsetAndMetadata> srcCommitOffsets = checkCommittedOffsets(topicRewardNotificationUpload, groupIdRewardNotificationUpload, payloads.size());
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

    //region errorUseCases
    @Override
    protected Pattern getErrorUseCaseIdPatternMatch() {
        return Pattern.compile("\"id\":\"id_([0-9]+)_?[^\"]*\"");
    }

    private final List<Pair<Supplier<String>, Consumer<ConsumerRecord<String,String>>>> errorUseCases = new ArrayList<>();
    {
        String useCaseJsonNotExpected = "{\"id\":\"id_0\",unexpectedStructure:0}";
        errorUseCases.add(Pair.of(
                () -> useCaseJsonNotExpected,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD_NOTIFICATION_FEEDBACK] Unexpected JSON", useCaseJsonNotExpected)
        ));

        String jsonNotValid = "{\"id\":\"id_1\",invalidJson";
        errorUseCases.add(Pair.of(
                () -> jsonNotValid,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD_NOTIFICATION_FEEDBACK] Unexpected JSON", jsonNotValid)
        ));
    }

    private void checkErrorMessageHeaders(ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload) {
        checkErrorMessageHeaders(topicRewardNotificationUpload, groupIdRewardNotificationUpload, errorMessage, errorDescription, expectedPayload,null);
    }
    //endregion
}
