package it.gov.pagopa.reward.notification.event.consumer;

import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.dto.mapper.IbanRequestDTO2RewardIbanMapper;
import it.gov.pagopa.reward.notification.model.RewardIban;
import it.gov.pagopa.reward.notification.repository.RewardIbanRepository;
import it.gov.pagopa.reward.notification.service.utils.IbanConstants;
import it.gov.pagopa.reward.notification.test.fakers.IbanOutcomeDTOFaker;
import it.gov.pagopa.reward.notification.test.fakers.IbanRequestDTOFaker;
import it.gov.pagopa.reward.notification.test.utils.TestUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.reward.notification.service.iban.RewardIbanServiceImpl=WARN",
        "logging.level.it.gov.pagopa.reward.notification.service.iban.outcome.IbanOutcomeMediatorServiceImpl=WARN",
})
class IbanOutcomeConsumerConfigTest extends BaseIntegrationTest {

    String userId = "USERID_%s";
    String initiativeId = "INITIATIVEID_%s";
    String iban = "IBAN_%s";
    @Autowired
    private RewardIbanRepository rewardIbanRepository;
    @Autowired
    private IbanRequestDTO2RewardIbanMapper requestDTO2RewardIbanMapper;

    @Test
    void ibanOutcomeConsumer(){
        int ibanNumber = 1000;
        int notValidIban = errorUseCases.size();
        int UnknownIban = 100;
        long maxWaitingMs = 30000;

        inizializingDB(ibanNumber);

        List<String> ibanPayloads = new ArrayList<>(ibanNumber+notValidIban+UnknownIban);
        ibanPayloads.addAll(buildCheckIbanOutcome(notValidIban+UnknownIban, notValidIban+UnknownIban+ibanNumber, true));
        ibanPayloads.addAll(IntStream.range(0,notValidIban).mapToObj(i -> errorUseCases.get(i).getFirst().get()).toList());
        ibanPayloads.addAll(buildCheckIbanOutcome(notValidIban, notValidIban+UnknownIban, false));

        long timeStart=System.currentTimeMillis();
        ibanPayloads.forEach(p -> publishIntoEmbeddedKafka(topicIbanOutcome,null,null, p));
        long timePublishingEnd=System.currentTimeMillis();

        long countSaved = waitForIbanStoreChanged(UnknownIban+notValidIban);
        Assertions.assertEquals(countSaved, notValidIban, UnknownIban);
        long timeEnd=System.currentTimeMillis();

        checkStatusDB(notValidIban+UnknownIban);
        checkErrorsPublished(notValidIban, maxWaitingMs, errorUseCases);

        System.out.printf("""
            ************************
            Time spent to send %d (%d + %d + %d) messages (from start): %d millis
            Time spent to assert iban store changed count (from previous check): %d millis
            ************************
            Test Completed in %d millis
            ************************
            """,
                ibanNumber + notValidIban + UnknownIban,
                ibanNumber,
                notValidIban,
                UnknownIban,
                timePublishingEnd-timeStart,
                timeEnd-timePublishingEnd,
                timeEnd-timeStart
        );

        long timeCommitCheckStart = System.currentTimeMillis();
        Map<TopicPartition, OffsetAndMetadata> srcCommitOffsets = checkCommittedOffsets(topicIbanOutcome, groupIdIbanOutcomeConsumer, ibanPayloads.size());
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

    //TODO add unknown logic
    private void checkStatusDB(int n) {
        Assertions.assertEquals(n,rewardIbanRepository.count().block());

        List<RewardIban> ibansInfo = IntStream.range(0, n)
                .mapToObj(i -> RewardIban.builder()
                        .id(userId.concat(initiativeId).formatted(i,i))
                        .userId(userId.formatted(i))
                        .initiativeId(initiativeId.formatted(i))
                        .iban(iban.formatted(i))
                        .build())
                .toList();
        ibansInfo.forEach(m -> {
            RewardIban result = rewardIbanRepository.findById(m.getId()).block();
            Assertions.assertNotNull(result);
            TestUtils.checkNotNullFields(result, "checkIbanOutcome");
            Assertions.assertEquals(m.getUserId(), result.getUserId());
            Assertions.assertEquals(m.getInitiativeId(), result.getInitiativeId());
            Assertions.assertEquals(m.getId(), result.getId());
            Assertions.assertEquals(m.getIban(), result.getIban());
        });
    }

    private List<String> buildCheckIbanOutcome(int bias, int n, boolean isKO) {
        return IntStream.range(bias, n)
                .mapToObj(i -> IbanOutcomeDTOFaker.mockInstanceBuilder(i)
                        .userId(userId.formatted(i))
                        .initiativeId(initiativeId.formatted(i))
                        .iban(iban.formatted(i))
                        .status(isKO ? IbanConstants.STATUS_KO : IbanConstants.STATUS_UNKNOWN_PSP)
                        .build())
                .map(TestUtils::jsonSerializer)
                .toList();
    }

    private void inizializingDB(int ibansIntoDB) {
        List<String> messagesIbanRequest = IntStream.range(0, ibansIntoDB)
                .mapToObj(i -> IbanRequestDTOFaker.mockInstanceBuilder(i)
                        .userId(userId.formatted(i))
                        .initiativeId(initiativeId.formatted(i))
                        .iban(iban.formatted(i))
                        .build())
                .map(TestUtils::jsonSerializer)
                .toList();

        messagesIbanRequest.forEach(m -> publishIntoEmbeddedKafka(topicIbanRequest,null, null, m));

        IbanRequestConsumerConfigTest.waitForIbanStored(ibansIntoDB,rewardIbanRepository);
    }


    protected Pattern getErrorUseCaseIdPatternMatch() {
        return Pattern.compile("\"userId\":\"USERID_([0-9]+)\"");
    }

    private final List<Pair<Supplier<String>, Consumer<ConsumerRecord<String,String>>>> errorUseCases = new ArrayList<>();
    {
        String useCaseJsonNotExpected = "{\"userId\":\"USERID_0\",unexpectedStructureForIban:0}";
        errorUseCases.add(Pair.of(
                () -> useCaseJsonNotExpected,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD_NOTIFICATION_IBAN_OUTCOME] Unexpected JSON", useCaseJsonNotExpected)
        ));

        String jsonNotValid = "{\"userId\":\"USERID_1\",invalidJsonForIban";
        errorUseCases.add(Pair.of(
                () -> jsonNotValid,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD_NOTIFICATION_IBAN_OUTCOME] Unexpected JSON", jsonNotValid)
        ));
    }

    private void checkErrorMessageHeaders(ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload) {
        checkErrorMessageHeaders(topicIbanOutcome, errorMessage, errorDescription, expectedPayload,null);
    }

    private long waitForIbanStoreChanged(int n) {
        return waitForIbanStoreChanged(n, rewardIbanRepository);
    }

    public static long waitForIbanStoreChanged(int n, RewardIbanRepository rewardIbanRepository) {
        long[] countSaved={0};
        //noinspection ConstantConditions
        waitFor(()->(countSaved[0]=rewardIbanRepository.count().block()) <= n, ()->"Expected %d saved iban, read %d".formatted(n, countSaved[0]), 60, 1000);
        return countSaved[0];
    }
}