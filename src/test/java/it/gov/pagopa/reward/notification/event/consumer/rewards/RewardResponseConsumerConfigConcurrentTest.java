package it.gov.pagopa.reward.notification.event.consumer.rewards;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.notification.enums.BeneficiaryType;
import it.gov.pagopa.reward.notification.enums.DepositType;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.Rewards;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.test.fakers.RewardTransactionDTOFaker;
import it.gov.pagopa.reward.notification.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

@Slf4j
class RewardResponseConsumerConfigConcurrentTest extends BaseRewardResponseConsumerConfigTest {

    private final int initiativeRewardedNumber = 8;

    private final int validTrx = 100;

    private final BigDecimal thresholdReward = BigDecimal.valueOf(7);
    private final int trxNumberInThreshold = INITIATIVE_THRESHOLD_VALUE_REFUND_THRESHOLD.divide(thresholdReward, 0, RoundingMode.CEILING).intValue();
    private final int totalThresholdNotifications = (int)Math.ceil((double)validTrx/trxNumberInThreshold);
    private final long completedThresholdCents = trxNumberInThreshold* CommonUtilities.euroToCents(thresholdReward);
    private final long lastThresholdCents = CommonUtilities.euroToCents(thresholdReward)*validTrx % completedThresholdCents;

    @Test
    void testConsumer() {
        int expectedRewardsNumber = validTrx * initiativeRewardedNumber;

        publishRewardRules();

        List<RewardTransactionDTO> trxs = new ArrayList<>(buildValidPayloads(validTrx));

        long totalSendMessages = trxs.size();

        long timePublishOnboardingStart = System.currentTimeMillis();
        trxs.forEach(p -> kafkaTestUtilitiesService.publishIntoEmbeddedKafka(topicRewardResponse, null,p.getUserId(), p));
        long timePublishingOnboardingRequest = System.currentTimeMillis() - timePublishOnboardingStart;

        long timeBeforeDbCheck = System.currentTimeMillis();

        Assertions.assertEquals(expectedRewardsNumber, waitForRewardsStored(expectedRewardsNumber));
        long timeEnd = System.currentTimeMillis();

        checkRewards(expectedRewardsNumber);
        checkRewardsNotification(trxs);

        System.out.printf("""
                        ************************
                        Time spent to send %d trx messages: %d millis
                        Time spent to consume reward responses: %d millis
                        ************************
                        Test Completed in %d millis
                        ************************
                        """,
                totalSendMessages,
                timePublishingOnboardingRequest,
                timeEnd - timeBeforeDbCheck,
                timeEnd - timePublishOnboardingStart
        );
    }

    private List<RewardTransactionDTO> buildValidPayloads(int validOnboardings) {
        return IntStream.range(0, validOnboardings)
                .mapToObj(i->mockInstance(i, validOnboardings-1))
                .toList();
    }

    private RewardTransactionDTO mockInstance(int bias, int lastBias) {
        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(bias);
        trx.setUserId("USERID");

        Reward exhaustedReward = new Reward(BigDecimal.valueOf(8));
        if(bias==lastBias){
            exhaustedReward.getCounters().setExhaustedBudget(true);
        }

        trx.setRewards(Map.of(
                INITIATIVE_ID_NOTIFY_DAILY, new Reward(BigDecimal.valueOf(2), BigDecimal.valueOf(1)),
                INITIATIVE_ID_NOTIFY_WEEKLY, new Reward(BigDecimal.valueOf(2)),
                INITIATIVE_ID_NOTIFY_MONTHLY, new Reward(BigDecimal.valueOf(3)),
                INITIATIVE_ID_NOTIFY_QUARTERLY, new Reward(BigDecimal.valueOf(4)),
                INITIATIVE_ID_NOTIFY_CLOSED, new Reward(BigDecimal.valueOf(5)),
                INITIATIVE_ID_NOTIFY_CLOSED_ALREADY_EXPIRED, new Reward(BigDecimal.valueOf(6)),
                INITIATIVE_ID_NOTIFY_THRESHOLD, new Reward(thresholdReward),
                INITIATIVE_ID_NOTIFY_EXHAUSTED, exhaustedReward
        ));
        return trx;
    }

    private void checkRewards(int expectedRewardsNumber) {
        List<Rewards> rewards = rewardsRepository.findAll().collectList().block();

        Assertions.assertNotNull(rewards);
        Assertions.assertEquals(expectedRewardsNumber, rewards.size());

        rewards.forEach(r-> {
            switch (r.getInitiativeId()) {
                case INITIATIVE_ID_NOTIFY_DAILY ->
                        assertRewards(r, r.getInitiativeId(), "USERID_INITIATIVEID_DAILY_%s".formatted(TOMORROW.format(Utils.FORMATTER_DATE)), TOMORROW, BigDecimal.valueOf(1), false);
                case INITIATIVE_ID_NOTIFY_WEEKLY ->
                        assertRewards(r, r.getInitiativeId(), "USERID_INITIATIVEID_WEEKLY_%s".formatted(NEXT_WEEK.format(Utils.FORMATTER_DATE)), NEXT_WEEK, BigDecimal.valueOf(2), false);
                case INITIATIVE_ID_NOTIFY_MONTHLY ->
                        assertRewards(r, r.getInitiativeId(), "USERID_INITIATIVEID_MONTHLY_%s".formatted(NEXT_MONTH.format(Utils.FORMATTER_DATE)), NEXT_MONTH, BigDecimal.valueOf(3), false);
                case INITIATIVE_ID_NOTIFY_QUARTERLY ->
                        assertRewards(r, r.getInitiativeId(), "USERID_INITIATIVEID_QUARTERLY_%s".formatted(NEXT_QUARTER.format(Utils.FORMATTER_DATE)), NEXT_QUARTER, BigDecimal.valueOf(4), false);
                case INITIATIVE_ID_NOTIFY_CLOSED ->
                        assertRewards(r, r.getInitiativeId(), "USERID_INITIATIVEID_CLOSED_%s".formatted(INITIATIVE_ENDDATE_NEXT_DAY.format(Utils.FORMATTER_DATE)), INITIATIVE_ENDDATE_NEXT_DAY, BigDecimal.valueOf(5), false);
                case INITIATIVE_ID_NOTIFY_CLOSED_ALREADY_EXPIRED ->
                        assertRewards(r, r.getInitiativeId(), "USERID_INITIATIVEID_CLOSED_ALREADY_EXPIRED_%s".formatted(TOMORROW.format(Utils.FORMATTER_DATE)), TOMORROW, BigDecimal.valueOf(6), false);
                case INITIATIVE_ID_NOTIFY_THRESHOLD -> {
                    Assertions.assertTrue(r.getNotificationId().startsWith("USERID_INITIATIVEID_THRESHOLD_"), "Unexpected notificationId: " + r.getNotificationId());
                    int progressive = Integer.parseInt(r.getNotificationId().substring(30));
                    assertRewards(r, r.getInitiativeId(), r.getNotificationId(), getExpectedThresholdNotificationDate(progressive), thresholdReward, false);
                }
                case INITIATIVE_ID_NOTIFY_EXHAUSTED ->
                        assertRewards(r, r.getInitiativeId(), "USERID_INITIATIVEID_EXHAUSTED_1", NEXT_WEEK, BigDecimal.valueOf(8), false);
                default -> throw new IllegalArgumentException("Unexpected initiativeId: " + r);
            }
        });
    }

    private void checkRewardsNotification(List<RewardTransactionDTO> trxs) {
        List<RewardsNotification> rewardsNotifications = Objects.requireNonNull(rewardsNotificationRepository.findAll().collectList().block());

        Assertions.assertNotNull(rewardsNotifications);
        Assertions.assertEquals(initiativeRewardedNumber + totalThresholdNotifications - 1, rewardsNotifications.size());

        rewardsNotifications.forEach(n-> {
            final RewardsNotification expectedNotification =
                    RewardsNotification.builder()
                            .externalId(n.getExternalId())
                            .initiativeId(n.getInitiativeId())
                            .initiativeName("INITIATIVE_NAME_" + n.getInitiativeId())
                            .organizationId("ORGANIZATION_ID_" + n.getInitiativeId())
                            .organizationFiscalCode("ORGANIZATION_VAT_" + n.getInitiativeId())
                            .beneficiaryId("USERID")
                            .beneficiaryType(BeneficiaryType.CITIZEN)
                            .progressive(1L)
                            .trxIds(trxs.stream().map(TransactionDTO::getId).toList())
                            .depositType(DepositType.PARTIAL)
                            .startDepositDate(TODAY)
                            .status(RewardNotificationStatus.TO_SEND)
                    .build();
            switch (n.getInitiativeId()) {
                case INITIATIVE_ID_NOTIFY_DAILY -> {
                    expectedNotification.setId("USERID_INITIATIVEID_DAILY_%s".formatted(TOMORROW.format(Utils.FORMATTER_DATE)));
                    expectedNotification.setNotificationDate(TOMORROW);
                    expectedNotification.setRewardCents(trxs.size()*100L);
                }
                case INITIATIVE_ID_NOTIFY_WEEKLY -> {
                    expectedNotification.setId("USERID_INITIATIVEID_WEEKLY_%s".formatted(NEXT_WEEK.format(Utils.FORMATTER_DATE)));
                    expectedNotification.setNotificationDate(NEXT_WEEK);
                    expectedNotification.setRewardCents(trxs.size()*200L);
                }
                case INITIATIVE_ID_NOTIFY_MONTHLY -> {
                    expectedNotification.setId("USERID_INITIATIVEID_MONTHLY_%s".formatted(NEXT_MONTH.format(Utils.FORMATTER_DATE)));
                    expectedNotification.setNotificationDate(NEXT_MONTH);
                    expectedNotification.setRewardCents(trxs.size()*300L);
                }
                case INITIATIVE_ID_NOTIFY_QUARTERLY -> {
                    expectedNotification.setId("USERID_INITIATIVEID_QUARTERLY_%s".formatted(NEXT_QUARTER.format(Utils.FORMATTER_DATE)));
                    expectedNotification.setNotificationDate(NEXT_QUARTER);
                    expectedNotification.setRewardCents(trxs.size()*400L);
                }
                case INITIATIVE_ID_NOTIFY_CLOSED -> {
                    expectedNotification.setId("USERID_INITIATIVEID_CLOSED_%s".formatted(INITIATIVE_ENDDATE_NEXT_DAY.format(Utils.FORMATTER_DATE)));
                    expectedNotification.setNotificationDate(INITIATIVE_ENDDATE_NEXT_DAY);
                    expectedNotification.setRewardCents(trxs.size()*500L);
                    expectedNotification.setDepositType(DepositType.FINAL);
                }
                case INITIATIVE_ID_NOTIFY_CLOSED_ALREADY_EXPIRED -> {
                    expectedNotification.setId("USERID_INITIATIVEID_CLOSED_ALREADY_EXPIRED_%s".formatted(TOMORROW.format(Utils.FORMATTER_DATE)));
                    expectedNotification.setNotificationDate(TOMORROW);
                    expectedNotification.setRewardCents(trxs.size()*600L);
                    expectedNotification.setDepositType(DepositType.FINAL);
                }
                case INITIATIVE_ID_NOTIFY_THRESHOLD -> {
                    expectedNotification.setProgressive(n.getProgressive());
                    expectedNotification.setId("USERID_INITIATIVEID_THRESHOLD_%d".formatted(n.getProgressive()));
                    expectedNotification.setNotificationDate(getExpectedThresholdNotificationDate(n.getProgressive()));
                    expectedNotification.setRewardCents(isLastThresholdNotification(n.getProgressive()) ? lastThresholdCents : completedThresholdCents);
                    expectedNotification.setDepositType(DepositType.PARTIAL);
                    expectedNotification.setTrxIds(expectedNotification.getTrxIds().stream()
                            .skip((n.getProgressive()-1)*trxNumberInThreshold)
                            .limit(trxNumberInThreshold)
                            .toList());
                }
                case INITIATIVE_ID_NOTIFY_EXHAUSTED -> {
                    expectedNotification.setId("USERID_INITIATIVEID_EXHAUSTED_1");
                    expectedNotification.setNotificationDate(NEXT_WEEK);
                    expectedNotification.setRewardCents(trxs.size()*800L);
                    expectedNotification.setDepositType(DepositType.FINAL);
                }
                default -> throw new IllegalArgumentException("Unexpected initiativeId: " + n);
            }

            Assertions.assertEquals(expectedNotification, n);
        });
    }

    private LocalDate getExpectedThresholdNotificationDate(long progressive) {
        return isLastThresholdNotification(progressive) ? null : NEXT_WEEK;
    }

    private boolean isLastThresholdNotification(long progressive) {
        return progressive == totalThresholdNotifications;
    }

}