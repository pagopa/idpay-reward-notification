package it.gov.pagopa.reward.notification.event.consumer.rewards;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.enums.DepositType;
import it.gov.pagopa.reward.notification.enums.RewardStatus;
import it.gov.pagopa.reward.notification.model.Rewards;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.RewardNotificationRuleEvaluatorService;
import it.gov.pagopa.reward.notification.service.utils.Utils;
import it.gov.pagopa.reward.notification.test.fakers.RewardTransactionDTOFaker;
import it.gov.pagopa.reward.notification.test.utils.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@Slf4j
class RewardResponseConsumerConfigTest extends BaseRewardResponseConsumerConfigTest {

    public static final String DUPLICATE_SUFFIX = "_DUPLICATE";

    @SpyBean
    private RewardNotificationRuleEvaluatorService rewardNotificationRuleEvaluatorServiceSpy;

    @Test
    void testConsumer() throws JsonProcessingException {
        int validTrx = 1000; // use even number
        int notValidTrx = errorUseCases.size();
        int duplicateTrx = Math.min(100, validTrx / 2); // we are sending as duplicated the first N transactions: error cases could invalidate duplicate check
        long maxWaitingMs = 30000;

        publishRewardRules();

        RewardTransactionDTO trxAlreadyProcessed = storeTrxAlreadyProcessed();
        RewardTransactionDTO trxNotRewarded = RewardTransactionDTOFaker.mockInstance(0);
        trxNotRewarded.setRewards(Collections.emptyMap());
        trxNotRewarded.setId("NOTREWARDEDTRX");

        List<String> trxs = new ArrayList<>(buildValidPayloads(errorUseCases.size(), validTrx / 2));
        trxs.addAll(IntStream.range(0, notValidTrx).mapToObj(i -> errorUseCases.get(i).getFirst().get()).toList());
        trxs.addAll(buildValidPayloads(errorUseCases.size() + (validTrx / 2) + notValidTrx, validTrx / 2));

        trxs.add(TestUtils.jsonSerializer(trxNotRewarded));
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

        Objects.requireNonNull(rewardsRepository.findAll().collectList().block())
                .forEach(this::checkResponse);

        verifyDuplicateCheck();

        verifyNotElaborated(trx -> trx.getId().equals(trxAlreadyProcessed.getId()));

        Assertions.assertEquals(
                objectMapper.writeValueAsString(prepare2Compare(expectedRewardNotifications.values())),
                objectMapper.writeValueAsString(prepare2Compare(Objects.requireNonNull(rewardsNotificationRepository.findAll().collectList().block())))
                );

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

        RewardsNotification rewardsNotification = storeRewardNotification(trxAlreadyProcessed, alreadyProcessedInitiative, TODAY, 100L, DepositType.PARTIAL, List.of(trxAlreadyProcessed.getId()));

        expectedRewardNotifications.put("ALREADY_PROCESSED_REWARD_CORRELATION_ID", rewardsNotification);

        return trxAlreadyProcessed;
    }

    private RewardsNotification storeRewardNotification(RewardTransactionDTO trx, String initiativeId, LocalDate notificationDate, long rewardCents, DepositType depositType, List<String> trxIds) {
        Rewards rewardAlreadyProcessed = storeReward(trx, initiativeId, RewardStatus.ACCEPTED);
        RewardsNotification rewardsNotification = buildRewardsNotificationExpected(rewardAlreadyProcessed.getNotificationId(), notificationDate, trx, 1L, initiativeId, rewardCents, depositType, null);
        rewardsNotification.setTrxIds(trxIds);
        return rewardsNotificationRepository.save(rewardsNotification).block();
    }

    private Rewards storeReward(RewardTransactionDTO trx, String initiativeId, RewardStatus rewardStatus) {
        Reward reward = trx.getRewards().get(initiativeId);
        Rewards storedRewards = rewardMapper.apply(initiativeId, reward, trx, null, "ALREADY_STORED_%s_%s_NOTIFICATIONID".formatted(trx.getId(), initiativeId));
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

    private void verifyDuplicateCheck() {
        verifyNotElaborated(trx -> trx.getSenderCode().endsWith(DUPLICATE_SUFFIX));
    }

    private void verifyNotElaborated(ArgumentMatcher<RewardTransactionDTO> matcher) {
        Mockito.verify(rewardNotificationRuleEvaluatorServiceSpy, Mockito.never()).retrieveAndEvaluate(Mockito.any(), Mockito.any(),
                Mockito.argThat(matcher),
                Mockito.any());
    }

    private RewardTransactionDTO mockInstance(int bias) {
        return useCases.get(bias % useCases.size()).getFirst().apply(bias);
    }

    Set<String> errorUseCasesStored_userid = Set.of("NOT_EXISTENT_INITIATIVE_ID_USER_ID");

    private void checkResponse(Rewards reward) {
        if (RewardStatus.ACCEPTED.equals(reward.getStatus())) {
            String trxId = reward.getUserId();
            int biasRetrieve = Integer.parseInt(trxId.substring(6));
            if(biasRetrieve >= errorUseCases.size()){
                useCases.get(biasRetrieve % useCases.size()).getSecond().accept(reward);
            }
        } else {
            Assertions.assertTrue(errorUseCasesStored_userid.contains(reward.getUserId()), "Invalid rejected reward: " + reward);
        }
    }

    //region useCases
    private final Map<String, RewardsNotification> expectedRewardNotifications = new HashMap<>();

    private final List<Pair<Function<Integer, RewardTransactionDTO>, Consumer<Rewards>>> useCases = List.of(
            // useCase 0: initiative daily notified
            Pair.of(
                    i -> {
                        String initiativeId = INITIATIVE_ID_NOTIFY_DAILY;
                        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstanceBuilder(i)
                                .rewards(Map.of(initiativeId, new Reward(BigDecimal.TEN)))
                                .build();
                        LocalDate expectedNotificationDate = TOMORROW;
                        String expectedNotificationId = "%s_%s_%s".formatted(trx.getUserId(), initiativeId, expectedNotificationDate.format(Utils.FORMATTER_DATE));
                        updateExpectedRewardNotification(expectedNotificationId, expectedNotificationDate, trx, initiativeId, 1000L, DepositType.PARTIAL);
                        return trx;
                    },
                    reward -> {
                        LocalDate expectedNotificationDate = TOMORROW;
                        String expectedNotificationId = "%s_%s_%s".formatted(reward.getUserId(), INITIATIVE_ID_NOTIFY_DAILY, expectedNotificationDate.format(Utils.FORMATTER_DATE));
                        assertRewards(reward, INITIATIVE_ID_NOTIFY_DAILY, expectedNotificationId, expectedNotificationDate, BigDecimal.TEN, true);
                    }
            ),
            // useCase 1: initiative weekly notified
            Pair.of(
                    i -> {
                        String initiativeId = INITIATIVE_ID_NOTIFY_WEEKLY;
                        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstanceBuilder(i)
                                .rewards(Map.of(initiativeId, new Reward(BigDecimal.valueOf(11))))
                                .build();
                        LocalDate expectedNotificationDate = NEXT_WEEK;
                        String expectedNotificationId = "%s_%s_%s".formatted(trx.getUserId(), initiativeId, expectedNotificationDate.format(Utils.FORMATTER_DATE));
                        updateExpectedRewardNotification(expectedNotificationId, expectedNotificationDate, trx, initiativeId, 1100L, DepositType.PARTIAL);
                        return trx;
                    },
                    reward -> assertRewards(reward, INITIATIVE_ID_NOTIFY_WEEKLY
                            , "%s_%s_%s".formatted(reward.getUserId(), INITIATIVE_ID_NOTIFY_WEEKLY, NEXT_WEEK.format(Utils.FORMATTER_DATE))
                            , NEXT_WEEK
                            , BigDecimal.valueOf(11), true)
            ),
            // useCase 2: initiative monthly notified
            Pair.of(
                    i -> {
                        String initiativeId = INITIATIVE_ID_NOTIFY_MONTHLY;
                        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstanceBuilder(i)
                                .rewards(Map.of(initiativeId, new Reward(BigDecimal.valueOf(12))))
                                .build();
                        LocalDate expectedNotificationDate = NEXT_MONTH;
                        String expectedNotificationId = "%s_%s_%s".formatted(trx.getUserId(), initiativeId, expectedNotificationDate.format(Utils.FORMATTER_DATE));
                        updateExpectedRewardNotification(expectedNotificationId, expectedNotificationDate, trx, initiativeId, 1200L, DepositType.PARTIAL);
                        return trx;
                    },
                    reward -> assertRewards(reward, INITIATIVE_ID_NOTIFY_MONTHLY
                            , "%s_%s_%s".formatted(reward.getUserId(), INITIATIVE_ID_NOTIFY_MONTHLY, NEXT_MONTH.format(Utils.FORMATTER_DATE))
                            , NEXT_MONTH
                            , BigDecimal.valueOf(12), true)
            ),
            // useCase 3: initiative quarterly notified
            Pair.of(
                    i -> {
                        String initiativeId = INITIATIVE_ID_NOTIFY_QUARTERLY;
                        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstanceBuilder(i)
                                .rewards(Map.of(initiativeId, new Reward(BigDecimal.valueOf(12))))
                                .build();
                        LocalDate expectedNotificationDate = NEXT_QUARTER;
                        String expectedNotificationId = "%s_%s_%s".formatted(trx.getUserId(), initiativeId, expectedNotificationDate.format(Utils.FORMATTER_DATE));
                        updateExpectedRewardNotification(expectedNotificationId, expectedNotificationDate, trx, initiativeId, 1200L, DepositType.PARTIAL);
                        return trx;
                    },
                    reward -> assertRewards(reward, INITIATIVE_ID_NOTIFY_QUARTERLY
                            , "%s_%s_%s".formatted(reward.getUserId(), INITIATIVE_ID_NOTIFY_QUARTERLY, NEXT_QUARTER.format(Utils.FORMATTER_DATE))
                            , NEXT_QUARTER
                            , BigDecimal.valueOf(12), true)
            ),
            // useCase 4: initiative closed notified
            Pair.of(
                    i -> {
                        String initiativeId = INITIATIVE_ID_NOTIFY_CLOSED;
                        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstanceBuilder(i)
                                .rewards(Map.of(initiativeId, new Reward(BigDecimal.valueOf(13))))
                                .build();
                        LocalDate expectedNotificationDate = INITIATIVE_ENDDATE_NEXT_DAY;
                        String expectedNotificationId = "%s_%s_%s".formatted(trx.getUserId(), initiativeId, expectedNotificationDate.format(Utils.FORMATTER_DATE));
                        updateExpectedRewardNotification(expectedNotificationId, expectedNotificationDate, trx, initiativeId, 1300L, DepositType.FINAL);
                        return trx;
                    },
                    reward -> {
                        LocalDate expectedNotificationDate = INITIATIVE_ENDDATE_NEXT_DAY;
                        String expectedNotificationId = "%s_%s_%s".formatted(reward.getUserId(), INITIATIVE_ID_NOTIFY_CLOSED, expectedNotificationDate.format(Utils.FORMATTER_DATE));
                        assertRewards(reward, INITIATIVE_ID_NOTIFY_CLOSED, expectedNotificationId, expectedNotificationDate, BigDecimal.valueOf(13), true);
                    }
            ),
            // useCase 5: initiative closed in past days notified
            Pair.of(
                    i -> {
                        String initiativeId = INITIATIVE_ID_NOTIFY_CLOSED_ALREADY_EXPIRED;
                        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstanceBuilder(i)
                                .rewards(Map.of(initiativeId, new Reward(BigDecimal.valueOf(14))))
                                .build();
                        LocalDate expectedNotificationDate = TOMORROW;
                        String expectedNotificationId = "%s_%s_%s".formatted(trx.getUserId(), initiativeId, expectedNotificationDate.format(Utils.FORMATTER_DATE));
                        updateExpectedRewardNotification(expectedNotificationId, expectedNotificationDate, trx, initiativeId, 1400L, DepositType.FINAL);
                        return trx;
                    },
                    reward -> {
                        LocalDate expectedNotificationDate = TOMORROW;
                        String expectedNotificationId = "%s_%s_%s".formatted(reward.getUserId(), INITIATIVE_ID_NOTIFY_CLOSED_ALREADY_EXPIRED, expectedNotificationDate.format(Utils.FORMATTER_DATE));
                        assertRewards(reward, INITIATIVE_ID_NOTIFY_CLOSED_ALREADY_EXPIRED, expectedNotificationId, expectedNotificationDate, BigDecimal.valueOf(14), true);
                    }
            ),

            // useCase 6: initiative threshold notified not past
            Pair.of(
                    i -> {
                        String initiativeId = INITIATIVE_ID_NOTIFY_THRESHOLD;
                        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstanceBuilder(i)
                                .rewards(Map.of(initiativeId, new Reward(BigDecimal.valueOf(15))))
                                .build();
                        LocalDate expectedNotificationDate = null;
//                        String expectedNotificationId = "%s_%s_%s".formatted(trx.getUserId(), initiativeId, expectedNotificationDate.format(Utils.FORMATTER_DATE));
//                        updateExpectedRewardNotification(expectedNotificationId, expectedNotificationDate, trx, initiativeId, 1500L, DepositType.PARTIAL);
                        // TODO mocked result, remove me after implementation
                        expectedRewardNotifications.put(trx.getUserId(),
                                RewardsNotification.builder()
                                        .id("%s_%s_THRESHOLD_NOTIFICATIONID".formatted(initiativeId, trx.getUserId()))
                                        .userId(trx.getUserId())
                                        .trxIds(List.of(trx.getId()))
                                        .build());
                        return trx;
                    },
                    reward -> assertRewards(reward, INITIATIVE_ID_NOTIFY_THRESHOLD
                            , "%s_%s_THRESHOLD_NOTIFICATIONID".formatted(INITIATIVE_ID_NOTIFY_THRESHOLD, reward.getUserId())
                            , null, BigDecimal.valueOf(15), true)
            ),

            // useCase 7: initiative threshold notified overflowed
            Pair.of(
                    i -> {
                        String initiativeId = INITIATIVE_ID_NOTIFY_THRESHOLD;
                        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstanceBuilder(i)
                                .rewards(Map.of(initiativeId, new Reward(BigDecimal.valueOf(100))))
                                .build();
                        LocalDate expectedNotificationDate = TOMORROW;
//                        String expectedNotificationId = "%s_%s_%s".formatted(trx.getUserId(), initiativeId, expectedNotificationDate.format(Utils.FORMATTER_DATE));
//                        updateExpectedRewardNotification(expectedNotificationId, expectedNotificationDate, trx, initiativeId, 10000L, DepositType.PARTIAL);
                        // TODO mocked result, remove me after implementation
                        expectedRewardNotifications.put(trx.getUserId(),
                                RewardsNotification.builder()
                                        .id("%s_%s_THRESHOLD_NOTIFICATIONID".formatted(initiativeId, trx.getUserId()))
                                        .userId(trx.getUserId())
                                        .trxIds(List.of(trx.getId()))
                                        .build());
                        return trx;
                    },
                    reward -> assertRewards(reward, INITIATIVE_ID_NOTIFY_THRESHOLD
                            , "%s_%s_THRESHOLD_NOTIFICATIONID".formatted(INITIATIVE_ID_NOTIFY_THRESHOLD, reward.getUserId())
                            , null // TODO , TOMORROW
                            , BigDecimal.valueOf(100), true)
            ),
            // useCase 8: initiative threshold notified overflowed after new trx
            Pair.of(
                    i -> {
                        String initiativeId = INITIATIVE_ID_NOTIFY_THRESHOLD;
                        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstanceBuilder(i)
                                .rewards(Map.of(initiativeId, new Reward(BigDecimal.TEN)))
                                .build();
//                        storeRewardNotification(trx, initiativeId, List.of(trx.getId()));
                        LocalDate expectedNotificationDate = TOMORROW;
//                        String expectedNotificationId = "%s_%s_%s".formatted(trx.getUserId(), initiativeId, expectedNotificationDate.format(Utils.FORMATTER_DATE));
//                        updateExpectedRewardNotification(expectedNotificationId, expectedNotificationDate, trx, initiativeId, 10900L, DepositType.PARTIAL);
                        // TODO mocked result, remove me after implementation
                        expectedRewardNotifications.put(trx.getUserId(),
                                RewardsNotification.builder()
                                        .id("%s_%s_THRESHOLD_NOTIFICATIONID".formatted(initiativeId, trx.getUserId()))
                                        .userId(trx.getUserId())
                                        .trxIds(List.of(trx.getId()))
                                        .build());
                        return trx;
                    },
                    reward -> assertRewards(reward, INITIATIVE_ID_NOTIFY_THRESHOLD
                            , "%s_%s_THRESHOLD_NOTIFICATIONID".formatted(INITIATIVE_ID_NOTIFY_THRESHOLD, reward.getUserId())
                            , null // TODO, TOMORROW
                            , BigDecimal.TEN, true)
            ),
            // useCase 9: initiative notified when initiative at budged exhausted, but not exhausted
            Pair.of(
                    i -> {
                        String initiativeId = INITIATIVE_ID_NOTIFY_EXHAUSTED;
                        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstanceBuilder(i)
                                .rewards(Map.of(initiativeId, new Reward(BigDecimal.TEN)))
                                .build();
                        LocalDate expectedNotificationDate = null;
//                        String expectedNotificationId = "%s_%s_%s".formatted(trx.getUserId(), initiativeId, expectedNotificationDate.format(Utils.FORMATTER_DATE));
//                        updateExpectedRewardNotification(expectedNotificationId, expectedNotificationDate, trx, initiativeId, 1000L, DepositType.FINAL);
                        // TODO mocked result, remove me after implementation
                        expectedRewardNotifications.put(trx.getUserId(),
                                RewardsNotification.builder()
                                        .id("%s_%s_BUDGET_EXHAUSTED_NOTIFICATIONID".formatted(initiativeId, trx.getUserId()))
                                        .userId(trx.getUserId())
                                        .trxIds(List.of(trx.getId()))
                                        .build());
                        return trx;
                    },
                    reward -> assertRewards(reward, INITIATIVE_ID_NOTIFY_EXHAUSTED
                            , "%s_%s_BUDGET_EXHAUSTED_NOTIFICATIONID".formatted(INITIATIVE_ID_NOTIFY_EXHAUSTED, reward.getUserId())
                            , null, BigDecimal.TEN, true)
            ),
            // useCase 10: initiative notified when budged exhausted, receiving exhausted
            Pair.of(
                    i -> {
                        String initiativeId = INITIATIVE_ID_NOTIFY_EXHAUSTED;
                        Reward reward = new Reward(BigDecimal.TEN);
                        reward.getCounters().setExhaustedBudget(true);
                        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstanceBuilder(i)
                                .rewards(Map.of(initiativeId, reward))
                                .build();
                        LocalDate expectedNotificationDate = TOMORROW;
//                        String expectedNotificationId = "%s_%s_%s".formatted(trx.getUserId(), initiativeId, expectedNotificationDate.format(Utils.FORMATTER_DATE));
//                        updateExpectedRewardNotification(expectedNotificationId, expectedNotificationDate, trx, initiativeId, 1000L, DepositType.FINAL);
                        // TODO mocked result, remove me after implementation
                        expectedRewardNotifications.put(trx.getUserId(),
                                RewardsNotification.builder()
                                        .id("%s_%s_BUDGET_EXHAUSTED_NOTIFICATIONID".formatted(initiativeId, trx.getUserId()))
                                        .userId(trx.getUserId())
                                        .trxIds(List.of(trx.getId()))
                                        .build());
                        return trx;
                    },
                    reward -> assertRewards(reward, INITIATIVE_ID_NOTIFY_EXHAUSTED
                            , "%s_%s_BUDGET_EXHAUSTED_NOTIFICATIONID".formatted(INITIATIVE_ID_NOTIFY_EXHAUSTED, reward.getUserId())
                            , null // TODO, TOMORROW
                            , BigDecimal.TEN, true)
            ),

            // useCase 11: initiative stored, but not processed -> thus new notification created
            Pair.of(
                    i -> {
                        String initiativeId = INITIATIVE_ID_NOTIFY_DAILY;
                        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstanceBuilder(i)
                                .rewards(Map.of(initiativeId, new Reward(BigDecimal.TEN)))
                                .build();

                        RewardsNotification storedNotification = storeRewardNotification(trx, initiativeId, TOMORROW, 1000L, DepositType.PARTIAL, Collections.emptyList());
                        expectedRewardNotifications.put("PREVIOUS_" + trx.getId(), storedNotification);

                        String expectedNotificationId = "%s_%s_%s".formatted(trx.getUserId(), initiativeId, TOMORROW.format(Utils.FORMATTER_DATE));
                        updateExpectedRewardNotification(expectedNotificationId, TOMORROW, trx, 2L, initiativeId, 1000L, DepositType.PARTIAL);

                        return trx;
                    },
                    reward -> {
                        LocalDate expectedNotificationDate = TOMORROW;
                        String expectedNotificationId = "%s_%s_%s".formatted(reward.getUserId(), INITIATIVE_ID_NOTIFY_DAILY, expectedNotificationDate.format(Utils.FORMATTER_DATE));
                        assertRewards(reward, INITIATIVE_ID_NOTIFY_DAILY, expectedNotificationId, expectedNotificationDate, BigDecimal.TEN, true);
                    }
            ),

            // useCase 12: initiative stored, but rejected -> thus now ACCEPTED and processed
            Pair.of(
                    i -> {
                        String initiativeId = INITIATIVE_ID_NOTIFY_DAILY;
                        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstanceBuilder(i)
                                .rewards(Map.of(initiativeId, new Reward(BigDecimal.TEN)))
                                .build();

                        storeReward(trx, initiativeId, RewardStatus.REJECTED);

                        LocalDate expectedNotificationDate = TOMORROW;
                        String expectedNotificationId = "%s_%s_%s".formatted(trx.getUserId(), initiativeId, expectedNotificationDate.format(Utils.FORMATTER_DATE));
                        updateExpectedRewardNotification(expectedNotificationId, expectedNotificationDate, trx, initiativeId, 1000L, DepositType.PARTIAL);

                        return trx;
                    },
                    reward -> {
                        LocalDate expectedNotificationDate = TOMORROW;
                        String expectedNotificationId = "%s_%s_%s".formatted(reward.getUserId(), INITIATIVE_ID_NOTIFY_DAILY, expectedNotificationDate.format(Utils.FORMATTER_DATE));
                        assertRewards(reward, INITIATIVE_ID_NOTIFY_DAILY, expectedNotificationId, expectedNotificationDate, BigDecimal.TEN, true);
                    }
            )
    );

    private RewardsNotification updateExpectedRewardNotification(String notificationId, LocalDate notificationDate, RewardTransactionDTO trx, String initiativeId, long rewardCents, DepositType depositType) {
        return updateExpectedRewardNotification(notificationId, notificationDate, trx, 1L, initiativeId, rewardCents, depositType);
    }
    private RewardsNotification updateExpectedRewardNotification(String notificationId, LocalDate notificationDate, RewardTransactionDTO trx, long progressive, String initiativeId, long rewardCents, DepositType depositType) {
        return expectedRewardNotifications.compute(notificationId, (id, n) ->
            buildRewardsNotificationExpected(notificationId, notificationDate, trx, progressive, initiativeId, rewardCents, depositType, n)
        );
    }
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

        final String failingUpdatingRewardNotificationUserId = "FAILING_UPDATING_REWARD_NOTIFICATION";
        String failingUpdatingRewardNotificationUseCase = TestUtils.jsonSerializer(
                RewardTransactionDTOFaker.mockInstanceBuilder(errorUseCases.size())
                        .userId(failingUpdatingRewardNotificationUserId)
                        .build()
        );
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doThrow(new RuntimeException("DUMMYEXCEPTION")).when(rewardNotificationRuleEvaluatorServiceSpy).retrieveAndEvaluate(Mockito.eq("INITIATIVEID"), Mockito.any(), Mockito.argThat(i -> failingUpdatingRewardNotificationUserId.equals(i.getUserId())), Mockito.any());
                    return failingUpdatingRewardNotificationUseCase;
                },
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD_NOTIFICATION] An error occurred evaluating transaction result", failingUpdatingRewardNotificationUseCase, failingUpdatingRewardNotificationUserId)
        ));

    }

    private void checkErrorMessageHeaders(ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload, String expectedKey) {
        checkErrorMessageHeaders(topicRewardResponse, errorMessage, errorDescription, expectedPayload, expectedKey);
    }
    //endregion
}