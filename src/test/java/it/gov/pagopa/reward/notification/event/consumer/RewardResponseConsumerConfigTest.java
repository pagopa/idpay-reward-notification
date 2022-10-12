package it.gov.pagopa.reward.notification.event.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.dto.AccumulatedAmountDTO;
import it.gov.pagopa.reward.notification.dto.mapper.RewardMapper;
import it.gov.pagopa.reward.notification.dto.rule.InitiativeGeneralDTO;
import it.gov.pagopa.reward.notification.dto.rule.InitiativeRefund2StoreDTO;
import it.gov.pagopa.reward.notification.dto.rule.InitiativeRefundRuleDTO;
import it.gov.pagopa.reward.notification.dto.rule.TimeParameterDTO;
import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.enums.RewardStatus;
import it.gov.pagopa.reward.notification.model.Rewards;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardNotificationRuleRepository;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.repository.RewardsRepository;
import it.gov.pagopa.reward.notification.service.LockServiceImpl;
import it.gov.pagopa.reward.notification.service.rewards.RewardsService;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.RewardNotificationRuleEvaluatorService;
import it.gov.pagopa.reward.notification.service.utils.Utils;
import it.gov.pagopa.reward.notification.test.fakers.RewardTransactionDTOFaker;
import it.gov.pagopa.reward.notification.test.utils.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.reward.notification.service.rewards.RewardsServiceImpl=WARN",
})
@Slf4j
class RewardResponseConsumerConfigTest extends BaseIntegrationTest {

    public static final String DUPLICATE_SUFFIX = "_DUPLICATE";

    @Autowired
    private RewardsRepository rewardsRepository;
    @Autowired
    private RewardsNotificationRepository rewardsNotificationRepository;
    // TODO uncomment when rewardsOrganizationExports logic has been implemented
//    @Autowired
//    private RewardsOrganizationExportsRepository rewardsOrganizationExportsRepository;

    @Autowired
    private RewardNotificationRuleRepository ruleRepository;

    @Autowired
    private LockServiceImpl lockService;
    @Autowired
    private RewardMapper rewardMapper;

    @SpyBean
    private RewardNotificationRuleEvaluatorService rewardNotificationRuleEvaluatorServiceSpy;

    // TODO remove after rewardNotification logic impl
    @SpyBean
    private RewardsService rewardsServiceSpy;

    @AfterEach
    void checkLockBouquet() throws NoSuchFieldException, IllegalAccessException {
        final Field locksField = LockServiceImpl.class.getDeclaredField("locks");
        locksField.setAccessible(true);
        @SuppressWarnings("unchecked") Map<Integer, Semaphore> locks = (Map<Integer, Semaphore>) locksField.get(lockService);
        locks.values().forEach(l -> Assertions.assertEquals(1, l.availablePermits()));
    }

    @AfterEach
    void clearData() {
        rewardsRepository.deleteAll().block();
        rewardsNotificationRepository.deleteAll().block();
        // TODO uncomment when rewardsOrganizationExports logic has been implemented
//        rewardsOrganizationExportsRepository.deleteAll().block();
        ruleRepository.deleteAll().block();
    }

    @Test
    void testTransactionProcessor() throws JsonProcessingException {
        int validTrx = 1000; // use even number
        int notValidTrx = errorUseCases.size();
        int duplicateTrx = Math.min(100, validTrx / 2); // we are sending as duplicated the first N transactions: error cases could invalidate duplicate check
        long maxWaitingMs = 30000;

        publishRewardRules();

        RewardTransactionDTO trxAlreadyProcessed = storeTrxAlreadyProcessed();

        // TODO removeme after rewardNotification logic build
        Mockito.doAnswer(a -> {
                    Rewards r = a.getArgument(0);
                    return rewardsNotificationRepository.save(
                                    RewardsNotification.builder()
                                            .id(r.getNotificationId())
                                            .trxIds(List.of(r.getTrxId()))
                                            .build())
                            .then((Mono<Rewards>) a.callRealMethod());
                })
                .when(rewardsServiceSpy)
                .save(Mockito.any());

        List<String> trxs = new ArrayList<>(buildValidPayloads(errorUseCases.size(), validTrx / 2));
        trxs.addAll(IntStream.range(0, notValidTrx).mapToObj(i -> errorUseCases.get(i).getFirst().get()).toList());
        trxs.addAll(buildValidPayloads(errorUseCases.size() + (validTrx / 2) + notValidTrx, validTrx / 2));

        trxs.add(TestUtils.jsonSerializer(trxAlreadyProcessed));
        int alreadyProcessed = 1;

        long totalSendMessages = trxs.size() + duplicateTrx;

        long timePublishOnboardingStart = System.currentTimeMillis();
        int[] i = new int[]{0};
        trxs.forEach(p -> {
            final String userId = Utils.readUserId(p);
            publishIntoEmbeddedKafka(topicRewardResponse, null, userId, p);

            // to test duplicate trx and their right processing order
            if (i[0] < duplicateTrx) {
                i[0]++;
                publishIntoEmbeddedKafka(topicRewardResponse, null, userId, p.replaceFirst("(senderCode\":\"[^\"]+)", "$1%s".formatted(DUPLICATE_SUFFIX)));
            }
        });
        long timePublishingOnboardingRequest = System.currentTimeMillis() - timePublishOnboardingStart;

        long timeBeforeDbCheck = System.currentTimeMillis();
        int expectedStored = validTrx + 1 + 1; // +1 duplicate +1 errorUseCases (no initiative)
        Assertions.assertEquals(expectedStored, waitForRewardsStored(expectedStored));
        long timeEnd = System.currentTimeMillis();

        rewardsRepository.findAll()
                .doOnNext(this::checkResponse)
                .collectList()
                .block();

        verifyDuplicateCheck();

        verifyNotElaborated(trx -> trx.getId().equals(trxAlreadyProcessed.getId()));

        // TODO uncomment when rewardNotification logic has been implemented
//        Assertions.assertEquals(
//                objectMapper.writeValueAsString(expectedRewardNotifications.values().stream()
//                        .sorted(Comparator.comparing(RewardNotifications::getUserId))
//                        .toList()),
//                objectMapper.writeValueAsString(Objects.requireNonNull(rewardsNotificationRepository.findAll().collectList().block()).stream()
//                        .sorted(Comparator.comparing(RewardNotifications::getUserId))
//                        .toList()));

        checkErrorsPublished(notValidTrx, maxWaitingMs, errorUseCases);

        System.out.printf("""
                        ************************
                        Time spent to send %d (%d + %d + %d + %d) trx messages: %d millis
                        Time spent to consume reward responses: %d millis
                        ************************
                        Test Completed in %d millis
                        ************************
                        """,
                totalSendMessages,
                validTrx,
                duplicateTrx,
                notValidTrx,
                alreadyProcessed,
                timePublishingOnboardingRequest,
                timeEnd - timeBeforeDbCheck,
                timeEnd - timePublishOnboardingStart
        );

        checkOffsets(totalSendMessages);
    }

    private RewardTransactionDTO storeTrxAlreadyProcessed() {
        String alreadyProcessedInitiative = "INITIATIVEID";

        RewardTransactionDTO trxAlreadyProcessed = RewardTransactionDTOFaker.mockInstance(1);
        trxAlreadyProcessed.setCorrelationId("ALREADY_PROCESSED_REWARD_CORRELATION_ID");
        trxAlreadyProcessed.setRewards(Map.of(alreadyProcessedInitiative, new Reward(BigDecimal.ONE)));

        storeRewardNotification(trxAlreadyProcessed, alreadyProcessedInitiative, List.of(trxAlreadyProcessed.getId()));

        return trxAlreadyProcessed;
    }

    private void storeRewardNotification(RewardTransactionDTO trx, String initiativeId, List<String> trxIds) {
        Rewards rewardAlreadyProcessed = storeReward(trx, initiativeId, RewardStatus.ACCEPTED);

        rewardsNotificationRepository.save(RewardsNotification.builder()
                .id(rewardAlreadyProcessed.getNotificationId())
                .trxIds(trxIds)
                .build()).block();
    }

    private Rewards storeReward(RewardTransactionDTO trx, String initiativeId, RewardStatus rewardStatus) {
        Reward reward = trx.getRewards().get(initiativeId);
        Rewards storedRewards = rewardMapper.apply(initiativeId, reward, trx, null, "%s_%s_NOTIFICATIONID".formatted(trx.getId(), initiativeId));
        storedRewards.setStatus(rewardStatus);
        rewardsRepository.save(storedRewards).block();
        return storedRewards;
    }

    private List<String> buildValidPayloads(int bias, int validOnboardings) {
        return IntStream.range(bias, bias + validOnboardings)
                .mapToObj(this::mockInstance)
                .map(TestUtils::jsonSerializer)
                .toList();
    }

    private long waitForRewardsStored(int n) {
        long[] countSaved = {0};
        //noinspection ConstantConditions
        waitFor(() -> (countSaved[0] = rewardsRepository.count().block()) >= n, () -> "Expected %d saved reward notification rules, read %d".formatted(n, countSaved[0]), 60, 1000);
        return countSaved[0];
    }

    private void verifyDuplicateCheck() {
        verifyNotElaborated(trx -> trx.getSenderCode().endsWith(DUPLICATE_SUFFIX));
    }

    private void verifyNotElaborated(ArgumentMatcher<RewardTransactionDTO> matcher) {
        Mockito.verify(rewardNotificationRuleEvaluatorServiceSpy, Mockito.never()).retrieveAndEvaluate(Mockito.any(), Mockito.any(),
                Mockito.argThat(matcher),
                Mockito.any());
    }

    // region initiative build
    private final LocalDate initiativeEndDate = LocalDate.now().plusDays(10);

    private static final String INITIATIVE_ID_NOTIFY_DAILY = "INITIATIVEID_DAILY";
    private static final String INITIATIVE_ID_NOTIFY_WEEKLY = "INITIATIVEID_WEEKLY";
    private static final String INITIATIVE_ID_NOTIFY_MONTHLY = "INITIATIVEID_MONTHLY";
    private static final String INITIATIVE_ID_NOTIFY_QUARTERLY = "INITIATIVEID_QUARTERLY";
    private static final String INITIATIVE_ID_NOTIFY_CLOSED = "INITIATIVEID_CLOSED";
    private static final String INITIATIVE_ID_NOTIFY_CLOSED_ALREADY_EXPIRED = "INITIATIVEID_CLOSED_ALREADY_EXPIRED";
    private static final String INITIATIVE_ID_NOTIFY_THRESHOLD = "INITIATIVEID_THRESHOLD";
    private static final String INITIATIVE_ID_NOTIFY_EXHAUSTED = "INITIATIVEID_EXHAUSTED";

    private void publishRewardRules() {
        List<InitiativeRefund2StoreDTO> rules = List.of(
                InitiativeRefund2StoreDTO.builder()
                        .initiativeId(INITIATIVE_ID_NOTIFY_DAILY)
                        .general(InitiativeGeneralDTO.builder()
                                .endDate(initiativeEndDate)
                                .build())
                        .refundRule(InitiativeRefundRuleDTO.builder()
                                .accumulatedAmount(AccumulatedAmountDTO.builder() // ignored because configured as timed
                                        .accumulatedType(AccumulatedAmountDTO.AccumulatedTypeEnum.THRESHOLD_REACHED)
                                        .refundThreshold(BigDecimal.ONE)
                                        .build())
                                .timeParameter(TimeParameterDTO.builder()
                                        .timeType(TimeParameterDTO.TimeTypeEnum.DAILY)
                                        .build())
                                .build())
                        .build(),
                InitiativeRefund2StoreDTO.builder()
                        .initiativeId(INITIATIVE_ID_NOTIFY_WEEKLY)
                        .general(InitiativeGeneralDTO.builder()
                                .endDate(initiativeEndDate)
                                .build())
                        .refundRule(InitiativeRefundRuleDTO.builder()
                                .timeParameter(TimeParameterDTO.builder()
                                        .timeType(TimeParameterDTO.TimeTypeEnum.WEEKLY)
                                        .build())
                                .build())
                        .build(),
                InitiativeRefund2StoreDTO.builder()
                        .initiativeId(INITIATIVE_ID_NOTIFY_MONTHLY)
                        .general(InitiativeGeneralDTO.builder()
                                .endDate(initiativeEndDate)
                                .build())
                        .refundRule(InitiativeRefundRuleDTO.builder()
                                .timeParameter(TimeParameterDTO.builder()
                                        .timeType(TimeParameterDTO.TimeTypeEnum.MONTHLY)
                                        .build())
                                .build())
                        .build(),
                InitiativeRefund2StoreDTO.builder()
                        .initiativeId(INITIATIVE_ID_NOTIFY_QUARTERLY)
                        .general(InitiativeGeneralDTO.builder()
                                .endDate(initiativeEndDate)
                                .build())
                        .refundRule(InitiativeRefundRuleDTO.builder()
                                .timeParameter(TimeParameterDTO.builder()
                                        .timeType(TimeParameterDTO.TimeTypeEnum.QUARTERLY)
                                        .build())
                                .build())
                        .build(),
                InitiativeRefund2StoreDTO.builder()
                        .initiativeId(INITIATIVE_ID_NOTIFY_CLOSED)
                        .general(InitiativeGeneralDTO.builder()
                                .endDate(initiativeEndDate)
                                .build())
                        .refundRule(InitiativeRefundRuleDTO.builder()
                                .timeParameter(TimeParameterDTO.builder()
                                        .timeType(TimeParameterDTO.TimeTypeEnum.CLOSED)
                                        .build())
                                .build())
                        .build(),
                InitiativeRefund2StoreDTO.builder()
                        .initiativeId(INITIATIVE_ID_NOTIFY_CLOSED_ALREADY_EXPIRED)
                        .general(InitiativeGeneralDTO.builder()
                                .endDate(LocalDate.now().minusDays(1))
                                .build())
                        .refundRule(InitiativeRefundRuleDTO.builder()
                                .timeParameter(TimeParameterDTO.builder()
                                        .timeType(TimeParameterDTO.TimeTypeEnum.CLOSED)
                                        .build())
                                .build())
                        .build(),

                InitiativeRefund2StoreDTO.builder()
                        .initiativeId(INITIATIVE_ID_NOTIFY_THRESHOLD)
                        .general(InitiativeGeneralDTO.builder()
                                .endDate(initiativeEndDate)
                                .build())
                        .refundRule(InitiativeRefundRuleDTO.builder()
                                .accumulatedAmount(AccumulatedAmountDTO.builder() // ignored because configured as timed
                                        .accumulatedType(AccumulatedAmountDTO.AccumulatedTypeEnum.THRESHOLD_REACHED)
                                        .refundThreshold(BigDecimal.valueOf(100))
                                        .build())
                                .build())
                        .build(),
                InitiativeRefund2StoreDTO.builder()
                        .initiativeId(INITIATIVE_ID_NOTIFY_EXHAUSTED)
                        .general(InitiativeGeneralDTO.builder()
                                .endDate(initiativeEndDate)
                                .build())
                        .refundRule(InitiativeRefundRuleDTO.builder()
                                .accumulatedAmount(AccumulatedAmountDTO.builder() // ignored because configured as timed
                                        .accumulatedType(AccumulatedAmountDTO.AccumulatedTypeEnum.BUDGET_EXHAUSTED)
                                        .build())
                                .build())
                        .build()
        );
        rules.forEach(i -> publishIntoEmbeddedKafka(topicInitiative2StoreConsumer, null, null, i));

        RefundRuleConsumerConfigTest.waitForInitiativeStored(rules.size(), ruleRepository);
    }
    //endregion

    private RewardTransactionDTO mockInstance(int bias) {
        return useCases.get(bias % useCases.size()).getFirst().apply(bias);
    }

    // TODO uncomment when rewardNotification logic has been implemented
//    protected void saveRewardNotification(String notificationId, RewardTransactionDTO trx, BigDecimal reward) {
//        rewardsNotificationRepository.save(
//                RewardsNotification.builder()
//                        .id(notificationId)
//                        .userId(trx.getUserId())
//                        .reward(reward)
//                        .build()).block();
//    }


    protected void checkOffsets(long expectedReadMessages) {
        long timeStart = System.currentTimeMillis();
        final Map<TopicPartition, OffsetAndMetadata> srcCommitOffsets = checkCommittedOffsets(topicRewardResponse, groupIdRewardResponse, expectedReadMessages, 20, 1000);
        long timeCommitChecked = System.currentTimeMillis();

        System.out.printf("""
                        ************************
                        Time occurred to check committed offset: %d millis
                        ************************
                        Source Topic Committed Offsets: %s
                        ************************
                        """,
                timeCommitChecked - timeStart,
                srcCommitOffsets
        );
    }

    Set<String> errorUseCasesStored_userid = Set.of("NOT_EXISTENT_INITIATIVE_ID_USER_ID");

    private void checkResponse(Rewards reward) {
        if (RewardStatus.ACCEPTED.equals(reward.getStatus())) {
            String trxId = reward.getUserId();
            int biasRetrieve = Integer.parseInt(trxId.substring(6));
            useCases.get(biasRetrieve % useCases.size()).getSecond().accept(reward);
            //        createRewardNotification()
        } else {
            Assertions.assertTrue(errorUseCasesStored_userid.contains(reward.getUserId()), "Invalid rejected reward: " + reward);
        }
    }

    //region useCases
    // TODO uncomment when rewardNotification logic has been implemented
//    private final Map<String, RewardsNotification> expectedRewardNotifications = new HashMap<>();

    private final List<Pair<Function<Integer, RewardTransactionDTO>, Consumer<Rewards>>> useCases = List.of(
            // initiative daily notified
            Pair.of(
                    i -> RewardTransactionDTOFaker.mockInstanceBuilder(i)
                            .rewards(Map.of(INITIATIVE_ID_NOTIFY_DAILY, new Reward(BigDecimal.TEN)))
                            .build(),
                    reward -> assertRewardNotification(reward, INITIATIVE_ID_NOTIFY_DAILY, LocalDate.now().plusDays(1), BigDecimal.TEN)
            ),
            // initiative weekly notified
            Pair.of(
                    i -> RewardTransactionDTOFaker.mockInstanceBuilder(i)
                            .rewards(Map.of(INITIATIVE_ID_NOTIFY_WEEKLY, new Reward(BigDecimal.TEN)))
                            .build(),
                    reward -> assertRewardNotification(reward, INITIATIVE_ID_NOTIFY_WEEKLY
                            , LocalDate.now().plusDays(1) // TODO calculate next week
                            , BigDecimal.TEN)
            ),
            // initiative quarterly notified
            Pair.of(
                    i -> RewardTransactionDTOFaker.mockInstanceBuilder(i)
                            .rewards(Map.of(INITIATIVE_ID_NOTIFY_QUARTERLY, new Reward(BigDecimal.TEN)))
                            .build(),
                    reward -> assertRewardNotification(reward, INITIATIVE_ID_NOTIFY_QUARTERLY
                            , LocalDate.now().plusDays(1) // TODO calculate next week
                            , BigDecimal.TEN)
            ),
            // initiative closed notified
            Pair.of(
                    i -> RewardTransactionDTOFaker.mockInstanceBuilder(i)
                            .rewards(Map.of(INITIATIVE_ID_NOTIFY_CLOSED, new Reward(BigDecimal.TEN)))
                            .build(),
                    reward -> assertRewardNotification(reward, INITIATIVE_ID_NOTIFY_CLOSED, initiativeEndDate.plusDays(1), BigDecimal.TEN)
            ),
            // initiative closed in past days notified
            Pair.of(
                    i -> RewardTransactionDTOFaker.mockInstanceBuilder(i)
                            .rewards(Map.of(INITIATIVE_ID_NOTIFY_CLOSED_ALREADY_EXPIRED, new Reward(BigDecimal.TEN)))
                            .build(),
                    reward -> assertRewardNotification(reward, INITIATIVE_ID_NOTIFY_CLOSED_ALREADY_EXPIRED, LocalDate.now().plusDays(1), BigDecimal.TEN)
            ),

            // initiative threshold notified not past
            Pair.of(
                    i -> RewardTransactionDTOFaker.mockInstanceBuilder(i)
                            .rewards(Map.of(INITIATIVE_ID_NOTIFY_THRESHOLD, new Reward(BigDecimal.TEN)))
                            .build(),
                    reward -> assertRewardNotification(reward, INITIATIVE_ID_NOTIFY_THRESHOLD, null, BigDecimal.TEN)
            ),

            // initiative threshold notified overflowed
            Pair.of(
                    i -> RewardTransactionDTOFaker.mockInstanceBuilder(i)
                            .rewards(Map.of(INITIATIVE_ID_NOTIFY_THRESHOLD, new Reward(BigDecimal.valueOf(100))))
                            .build(),
                    reward -> assertRewardNotification(reward, INITIATIVE_ID_NOTIFY_THRESHOLD, LocalDate.now().plusDays(1), BigDecimal.valueOf(100))
            ),
            // initiative threshold notified overflowed after new trx
            Pair.of(
                    i -> {
                        // TODO store into rewardNotification a reward of 99 for current trx
//                        createRewardNotification()
                        return RewardTransactionDTOFaker.mockInstanceBuilder(i)
                                .rewards(Map.of(INITIATIVE_ID_NOTIFY_THRESHOLD, new Reward(BigDecimal.TEN)))
                                .build();
                    },
                    reward -> assertRewardNotification(reward, INITIATIVE_ID_NOTIFY_THRESHOLD, LocalDate.now().plusDays(1), BigDecimal.valueOf(109))
            ),
            // initiative notified when budged exhausted
            Pair.of(
                    i -> RewardTransactionDTOFaker.mockInstanceBuilder(i)
                            .rewards(Map.of(INITIATIVE_ID_NOTIFY_THRESHOLD, new Reward(BigDecimal.TEN)))
                            .build(),
                    reward -> assertRewardNotification(reward, INITIATIVE_ID_NOTIFY_EXHAUSTED, null, BigDecimal.TEN)
            ),
            // initiative notified when budged exhausted, receiving exhausted
            Pair.of(
                    i -> {
                        RewardTransactionDTO reward = RewardTransactionDTOFaker.mockInstanceBuilder(i)
                                .rewards(Map.of(INITIATIVE_ID_NOTIFY_THRESHOLD, new Reward(BigDecimal.TEN)))
                                .build();
                        reward.getRewards().get(INITIATIVE_ID_NOTIFY_THRESHOLD).getCounters().setExhaustedBudget(true);
                        return reward;
                    },
                    reward -> assertRewardNotification(reward, INITIATIVE_ID_NOTIFY_EXHAUSTED, LocalDate.now().plusDays(1), BigDecimal.TEN)
            ),

            // initiative stored, but not processed
            Pair.of(
                    i -> {
                        Reward reward = new Reward(BigDecimal.TEN);
                        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstanceBuilder(i)
                                .rewards(Map.of(INITIATIVE_ID_NOTIFY_DAILY, reward))
                                .build();

                        storeRewardNotification(trx, INITIATIVE_ID_NOTIFY_DAILY, Collections.emptyList());

                        return trx;
                    },
                    reward -> assertRewardNotification(reward, INITIATIVE_ID_NOTIFY_DAILY, LocalDate.now().plusDays(1), BigDecimal.TEN)
            ),

            // initiative stored, but rejected
            Pair.of(
                    i -> {
                        Reward reward = new Reward(BigDecimal.TEN);
                        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstanceBuilder(i)
                                .rewards(Map.of(INITIATIVE_ID_NOTIFY_DAILY, reward))
                                .build();

                        storeReward(trx, INITIATIVE_ID_NOTIFY_DAILY, RewardStatus.REJECTED);

                        return trx;
                    },
                    reward -> assertRewardNotification(reward, INITIATIVE_ID_NOTIFY_DAILY, LocalDate.now().plusDays(1), BigDecimal.TEN)
            )
    );

    private void assertRewardNotification(Rewards evaluation, String expectedInitiativeId, LocalDate expectedNotificationDate, BigDecimal expectedReward) {
        // TODO search rewardNotification and assert values
    }

// TODO uncomment when rewardNotification logic implemented
//    private RewardsNotification createRewardNotification(String notificationId, LocalDate notificationDate, BigDecimal reward) {
//        return expectedRewardNotifications.computeIfAbsent(notificationId, u -> new RewardsNotification(notificationId));
//    }
    //endregion

    //region not valid useCases
    // all use cases configured must have a unique id recognized by the regexp getErrorUseCaseIdPatternMatch
    protected Pattern getErrorUseCaseIdPatternMatch() {
        return Pattern.compile("\"correlationId\":\"CORRELATIONID([0-9]+)\"");
    }

    private final List<Pair<Supplier<String>, Consumer<ConsumerRecord<String, String>>>> errorUseCases = new ArrayList<>();

    {
        String useCaseJsonNotExpected = "{\"correlationId\":\"CORRELATIONID0\",unexpectedStructure:0}";
        errorUseCases.add(Pair.of(
                () -> useCaseJsonNotExpected,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD_NOTIFICATION] Unexpected JSON", useCaseJsonNotExpected, null)
        ));

        String jsonNotValid = "{\"correlationId\":\"CORRELATIONID1\",invalidJson";
        errorUseCases.add(Pair.of(
                () -> jsonNotValid,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD_NOTIFICATION] Unexpected JSON", jsonNotValid, null)
        ));

        final String notExistentInitiativeUserId = "NOT_EXISTENT_INITIATIVE_ID_USER_ID";
        String notExistentInitiativeUseCase = TestUtils.jsonSerializer(
                RewardTransactionDTOFaker.mockInstanceBuilder(errorUseCases.size(), "NOT_EXISTENT_INITIATIVE_ID")
                        .userId(notExistentInitiativeUserId)
                        .build()
        );
        errorUseCases.add(Pair.of(
                () -> notExistentInitiativeUseCase,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD_NOTIFICATION] Cannot find initiative having id: NOT_EXISTENT_INITIATIVE_ID", notExistentInitiativeUseCase, notExistentInitiativeUserId)
        ));

        // TODO uncomment and configure when rewardNotification logic has been implemented
//        final String failingUpdatingRewardNotification = "FAILING_UPDATING_REWARD_NOTIFICATION";
//        String failingUpdatingRewardNotificationUseCase = TestUtils.jsonSerializer(
//                RewardTransactionDTOFaker.mockInstanceBuilder(errorUseCases.size())
//                        .userId(failingUpdatingRewardNotification)
//                        .build()
//        );
//        errorUseCases.add(Pair.of(
//                () -> {
//                    Mockito.doThrow(new RuntimeException("DUMMYEXCEPTION")).when(userInitiativeCountersUpdateServiceSpy).update(Mockito.any(), Mockito.argThat(i -> failingCounterUpdateUserId.equals(i.getUserId())));
//                    return failingUpdatingRewardNotificationUseCase;
//                },
//                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD] An error occurred evaluating transaction", failingUpdatingRewardNotificationUseCase, failingCounterUpdateUserId)
//        ));

    }


    private void checkErrorMessageHeaders(ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload, String expectedKey) {
        checkErrorMessageHeaders(topicRewardResponse, errorMessage, errorDescription, expectedPayload, expectedKey);
    }
    //endregion
}