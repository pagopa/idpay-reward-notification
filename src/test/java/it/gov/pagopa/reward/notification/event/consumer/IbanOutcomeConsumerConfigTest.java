package it.gov.pagopa.reward.notification.event.consumer;

import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.model.RewardIban;
import it.gov.pagopa.reward.notification.repository.RewardIbanRepository;
import it.gov.pagopa.reward.notification.service.ErrorNotifierServiceImpl;
import it.gov.pagopa.reward.notification.service.utils.IbanConstants;
import it.gov.pagopa.reward.notification.test.fakers.IbanOutcomeDTOFaker;
import it.gov.pagopa.reward.notification.test.utils.TestUtils;
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
        "logging.level.it.gov.pagopa.reward.notification.service.iban.outcome.IbanOutcomeMediatorServiceImpl=WARN",
})
class IbanOutcomeConsumerConfigTest extends BaseIntegrationTest {

    private final String userId = "USERID_%s";
    private final String initiativeId = "INITIATIVEID_%s";
    private final String iban = "IBAN_%s";

    @SpyBean
    private RewardIbanRepository rewardIbanRepository;

    @AfterEach
    void cleanData(){
        rewardIbanRepository.deleteAll().block();
    }

    @Test
    void ibanOutcomeConsumer(){
        int ibanOkAndUnknown = 1000;
        int notValidIban = errorUseCases.size();
        int ibanKO = 500;
        long maxWaitingMs = 30000;

        List<String> ibanPayloads = new ArrayList<>(notValidIban+ibanOkAndUnknown);
        ibanPayloads.addAll(IntStream.range(0,notValidIban).mapToObj(i -> errorUseCases.get(i).getFirst().get()).toList());
        ibanPayloads.addAll(buildCheckIbanOutcome(notValidIban, notValidIban+ibanOkAndUnknown, false));

        List<String> ibanPayloadsKO = buildCheckIbanOutcome(notValidIban, notValidIban+ibanKO, true);

        long timeStart=System.currentTimeMillis();
        ibanPayloads.forEach(p -> publishIntoEmbeddedKafka(topicIbanOutcome,null,null, p));
        long timePublishingOKAndUnknownEnd=System.currentTimeMillis();
        waitForIbanStoreChanged(ibanOkAndUnknown);

        long timePublishingKoStart=System.currentTimeMillis();
        ibanPayloadsKO.forEach(p -> publishIntoEmbeddedKafka(topicIbanOutcome,null,null, p));
        publishIntoEmbeddedKafka(topicIbanOutcome, List.of(new RecordHeader(ErrorNotifierServiceImpl.ERROR_MSG_HEADER_APPLICATION_NAME, "OTHERAPPNAME".getBytes(StandardCharsets.UTF_8))), null, "OTHERAPPMESSAGE");
        long timePublishingEnd=System.currentTimeMillis();

        long countSaved = waitForIbanStoreChanged(ibanOkAndUnknown-ibanKO);
        Assertions.assertEquals(ibanOkAndUnknown-ibanKO, countSaved);
        long timeEnd=System.currentTimeMillis();

        checkStatusDB(notValidIban, ibanOkAndUnknown, ibanKO);
        checkErrorsPublished(notValidIban, maxWaitingMs, errorUseCases);

        System.out.printf("""
            ************************
            Time spent to send %d (%d + %d + %d) messages (from start): %d millis
            Time spent to assert iban store changed count (from previous check): %d millis
            ************************
            Test Completed in %d millis
            ************************
            """,
                notValidIban  + ibanOkAndUnknown + ibanKO +1,
                notValidIban,
                ibanOkAndUnknown,
                ibanKO,
                (timePublishingOKAndUnknownEnd - timeStart) + (timePublishingEnd - timePublishingKoStart),
                timeEnd-timePublishingEnd,
                timeEnd-timeStart
        );

        long timeCommitCheckStart = System.currentTimeMillis();
        Map<TopicPartition, OffsetAndMetadata> srcCommitOffsets = checkCommittedOffsets(topicIbanOutcome, groupIdIbanOutcomeConsumer, ibanPayloads.size()+ibanPayloadsKO.size()+1); // +1 due to other applicationName useCase
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

    private void checkStatusDB(int errorCaseNumber, int ibanOkAndUnknown, int ibanKO) {
        List<RewardIban> ibansInfo = IntStream.range(errorCaseNumber+ibanKO, errorCaseNumber+ibanOkAndUnknown)
                .mapToObj(i -> RewardIban.builder()
                        .id(userId.concat(initiativeId).formatted(i,i))
                        .userId(userId.formatted(i))
                        .initiativeId(initiativeId.formatted(i))
                        .iban(iban.formatted(i))
                        .checkIbanOutcome(i%2==0? IbanConstants.STATUS_OK : IbanConstants.STATUS_UNKNOWN_PSP)
                        .build())
                .toList();
        ibansInfo.forEach(m -> {
            RewardIban result = rewardIbanRepository.findById(m.getId()).block();
            Assertions.assertNotNull(result);
            TestUtils.checkNotNullFields(result);

            Assertions.assertEquals(m.getUserId(), result.getUserId());
            Assertions.assertEquals(m.getInitiativeId(), result.getInitiativeId());
            Assertions.assertEquals(m.getId(), result.getId());
            Assertions.assertEquals(m.getIban(), result.getIban());
            Assertions.assertEquals(m.getCheckIbanOutcome(), result.getCheckIbanOutcome());
        });
    }

    private List<String> buildCheckIbanOutcome(int bias, int n, boolean isKO) {
        return IntStream.range(bias, n)
                .mapToObj(i -> {
                    String status;
                    if(isKO){
                        status = IbanConstants.STATUS_KO;
                    } else if(i%2==0){
                        status = IbanConstants.STATUS_OK;
                    }else {
                        status = IbanConstants.STATUS_UNKNOWN_PSP;
                    }

                    return IbanOutcomeDTOFaker.mockInstanceBuilder(i)
                            .userId(userId.formatted(i))
                            .initiativeId(initiativeId.formatted(i))
                            .iban(iban.formatted(i))
                            .status(status)
                            .build();
                })
                .map(TestUtils::jsonSerializer)
                .toList();
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

        final String failingUpdatingIban = "FAILING_UPDATING";
        String failingUpdatingUseCase = TestUtils.jsonSerializer(
                IbanOutcomeDTOFaker.mockInstanceBuilder(errorUseCases.size())
                        .userId("USERID_2")
                        .iban(failingUpdatingIban)
                        .build()
        );
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doThrow(new RuntimeException("DUMMYEXCEPTION")).when(rewardIbanRepository).save(Mockito.argThat(i -> failingUpdatingIban.equals(i.getIban())));
                    return failingUpdatingUseCase;
                },
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD_NOTIFICATION_IBAN_OUTCOME] An error occurred evaluating iban", failingUpdatingUseCase)
        ));
    }

    private void checkErrorMessageHeaders(ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload) {
        checkErrorMessageHeaders(topicIbanOutcome, groupIdIbanOutcomeConsumer, errorMessage, errorDescription, expectedPayload,null);
    }

    private long waitForIbanStoreChanged(int n) {
        return waitForIbanStoreChanged(n, rewardIbanRepository);
    }

    public static long waitForIbanStoreChanged(int n, RewardIbanRepository rewardIbanRepository) {
        long[] countSaved={0};
        //noinspection ConstantConditions
        waitFor(()->(countSaved[0]=rewardIbanRepository.findAll().filter(r->r.getCheckIbanOutcome()!=null).collectList().block().size()) >= n, ()->"Expected %d saved iban, read %d".formatted(n, countSaved[0]), 60, 1000);
        return countSaved[0];
    }
}