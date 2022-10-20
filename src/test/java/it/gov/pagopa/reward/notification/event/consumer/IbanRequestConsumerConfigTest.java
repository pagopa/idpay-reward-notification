package it.gov.pagopa.reward.notification.event.consumer;

import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.repository.RewardIbanRepository;
import it.gov.pagopa.reward.notification.service.ErrorNotifierServiceImpl;
import it.gov.pagopa.reward.notification.test.fakers.IbanRequestDTOFaker;
import it.gov.pagopa.reward.notification.test.utils.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.reward.notification.service.iban.RewardIbanServiceImpl=WARN",
        "logging.level.it.gov.pagopa.reward.notification.service.iban.request.IbanRequestMediatorServiceImpl=WARN",
})
@Slf4j
class IbanRequestConsumerConfigTest extends BaseIntegrationTest {

    @SpyBean
    private RewardIbanRepository rewardIbanRepository;

    @AfterEach
    void cleanData(){
        rewardIbanRepository.deleteAll().block();
    }

    @Test
    void testRewardIbanPersistence(){
        int validIban = 1000;
        int notValidIban = errorUseCases.size();
        long maxWaitingMs = 30000;

        List<String> ibanPayloads = new ArrayList<>();
        ibanPayloads.addAll(buildValidPayloads(notValidIban, validIban/2));
        ibanPayloads.addAll(IntStream.range(0,notValidIban).mapToObj(i -> errorUseCases.get(i).getFirst().get()).toList());
        ibanPayloads.addAll(buildValidPayloads(notValidIban + (validIban / 2) + notValidIban, validIban / 2));

        long timeStart=System.currentTimeMillis();
        ibanPayloads.forEach(i->publishIntoEmbeddedKafka(topicIbanRequest, null, null, i));
        publishIntoEmbeddedKafka(topicIbanRequest, List.of(new RecordHeader(ErrorNotifierServiceImpl.ERROR_MSG_HEADER_APPLICATION_NAME, "OTHERAPPNAME".getBytes(StandardCharsets.UTF_8))), null, "OTHERAPPMESSAGE");

        long timePublishingEnd=System.currentTimeMillis();

        long countSaved = waitForIbanStored(validIban);
        long timeEnd=System.currentTimeMillis();

        Assertions.assertEquals(validIban, countSaved);
        checkErrorsPublished(notValidIban, maxWaitingMs, errorUseCases);

        System.out.printf("""
            ************************
            Time spent to send %d (%d + %d) messages (from start): %d millis
            Time spent to assert iban stored count (from previous check): %d millis
            ************************
            Test Completed in %d millis
            ************************
            """,
                validIban + notValidIban,
                validIban,
                notValidIban,
                timePublishingEnd-timeStart,
                timeEnd-timePublishingEnd,
                timeEnd-timeStart
        );

        long timeCommitCheckStart = System.currentTimeMillis();
        Map<TopicPartition, OffsetAndMetadata> srcCommitOffsets = checkCommittedOffsets(topicIbanRequest, groupIdIbanRequestConsumer, ibanPayloads.size()+1); // +1 due to other applicationName useCase
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

    private List<String> buildValidPayloads(int bias, int n) {
        return IntStream.range(bias, bias+n)
                .mapToObj(IbanRequestDTOFaker::mockInstance)
                .map(TestUtils::jsonSerializer)
                .toList();
    }
    private long waitForIbanStored(int n) {
        return waitForIbanStored(n, rewardIbanRepository);
    }

    public static long waitForIbanStored(int n, RewardIbanRepository rewardIbanRepository) {
        long[] countSaved={0};
        //noinspection ConstantConditions
        waitFor(()->(countSaved[0]=rewardIbanRepository.count().block()) >= n, ()->"Expected %d saved iban, read %d".formatted(n, countSaved[0]), 60, 1000);
        return countSaved[0];
    }

    protected Pattern getErrorUseCaseIdPatternMatch() {
        return Pattern.compile("\"userId\":\"USERID_([0-9]+)\"");
    }

    private final List<Pair<Supplier<String>, Consumer<ConsumerRecord<String,String>>>> errorUseCases = new ArrayList<>();
    {
        String useCaseJsonNotExpected = "{\"userId\":\"USERID_0\",unexpectedStructureForIban:0}";
        errorUseCases.add(Pair.of(
                () -> useCaseJsonNotExpected,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD_NOTIFICATION_IBAN_REQUEST] Unexpected JSON", useCaseJsonNotExpected)
        ));

        String jsonNotValid = "{\"userId\":\"USERID_1\",invalidJsonForIban";
        errorUseCases.add(Pair.of(
                () -> jsonNotValid,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD_NOTIFICATION_IBAN_REQUEST] Unexpected JSON", jsonNotValid)
        ));

        final String failingUpdatingIban = "FAILING_UPDATING";
        String failingUpdatingUseCase = TestUtils.jsonSerializer(
                IbanRequestDTOFaker.mockInstanceBuilder(errorUseCases.size())
                        .userId("USERID_2")
                        .iban(failingUpdatingIban)
                        .build()
        );
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doThrow(new RuntimeException("DUMMYEXCEPTION")).when(rewardIbanRepository).save(Mockito.argThat(i -> failingUpdatingIban.equals(i.getIban())));
                    return failingUpdatingUseCase;
                },
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD_NOTIFICATION_IBAN_REQUEST] An error occurred evaluating iban", failingUpdatingUseCase)
        ));
    }

    private void checkErrorMessageHeaders(ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload) {
        checkErrorMessageHeaders(topicIbanRequest, groupIdIbanRequestConsumer, errorMessage, errorDescription, expectedPayload,null);
    }
}