package it.gov.pagopa.reward.notification.event.consumer.rewards;

import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.model.Rewards;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.service.utils.Utils;
import it.gov.pagopa.reward.notification.test.fakers.RewardTransactionDTOFaker;
import it.gov.pagopa.reward.notification.test.utils.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

@Slf4j
class RewardResponseConsumerConfigConcurrentTest extends BaseRewardResponseConsumerConfigTest {

    @Test
    void testConsumer() {
        int validTrx = 1000;
        long maxWaitingMs = 30000;

        publishRewardRules();

        List<String> trxs = new ArrayList<>(buildValidPayloads(validTrx));

        long totalSendMessages = trxs.size();

        long timePublishOnboardingStart = System.currentTimeMillis();
        trxs.forEach(p -> publishIntoEmbeddedKafka(topicRewardResponse, null, Utils.readUserId(p), p));
        long timePublishingOnboardingRequest = System.currentTimeMillis() - timePublishOnboardingStart;

        long timeBeforeDbCheck = System.currentTimeMillis();
        Assertions.assertEquals(validTrx, waitForRewardsStored(validTrx));
        long timeEnd = System.currentTimeMillis();

        checkRewards(validTrx);
        checkRewardsNotification(validTrx);

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

    private List<String> buildValidPayloads(int validOnboardings) {
        return IntStream.range(0, validOnboardings)
                .mapToObj(i->mockInstance(i, validOnboardings))
                .map(TestUtils::jsonSerializer)
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
                INITIATIVE_ID_NOTIFY_CLOSED_ALREADY_EXPIRED, new Reward(BigDecimal.valueOf(6))
//  TODO              INITIATIVE_ID_NOTIFY_THRESHOLD, new Reward(BigDecimal.valueOf(7)),
//  TODO              INITIATIVE_ID_NOTIFY_EXHAUSTED, exhaustedReward
        ));
        return trx;
    }

    private void checkRewards(int validTrx) {
        List<Rewards> rewards = rewardsRepository.findAll().collectList().block();

        Assertions.assertNotNull(rewards);
        Assertions.assertEquals(validTrx*8, rewards.size());

        rewards.forEach(r-> {
            switch (r.getInitiativeId()) {
                case INITIATIVE_ID_NOTIFY_DAILY ->
                        assertRewardNotification(r, r.getInitiativeId(), "USERID_INITIATIVEID_DAILY_%s".formatted(TOMORROW.format(Utils.FORMATTER_DATE)), TOMORROW, BigDecimal.valueOf(1));
                case INITIATIVE_ID_NOTIFY_WEEKLY ->
                        assertRewardNotification(r, r.getInitiativeId(), "USERID_INITIATIVEID_WEEKLY_%s".formatted(NEXT_WEEK.format(Utils.FORMATTER_DATE)), NEXT_WEEK, BigDecimal.valueOf(2));
                case INITIATIVE_ID_NOTIFY_MONTHLY ->
                        assertRewardNotification(r, r.getInitiativeId(), "USERID_INITIATIVEID_MONTHLY_%s".formatted(NEXT_MONTH.format(Utils.FORMATTER_DATE)), NEXT_MONTH, BigDecimal.valueOf(3));
                case INITIATIVE_ID_NOTIFY_QUARTERLY ->
                        assertRewardNotification(r, r.getInitiativeId(), "USERID_INITIATIVEID_QUARTERLY_%s".formatted(NEXT_QUARTER.format(Utils.FORMATTER_DATE)), NEXT_QUARTER, BigDecimal.valueOf(4));
                case INITIATIVE_ID_NOTIFY_CLOSED ->
                        assertRewardNotification(r, r.getInitiativeId(), "USERID_INITIATIVEID_CLOSED_%s".formatted(initiativeEndDate.plusDays(1).format(Utils.FORMATTER_DATE)), initiativeEndDate.plusDays(1), BigDecimal.valueOf(5));
                case INITIATIVE_ID_NOTIFY_CLOSED_ALREADY_EXPIRED ->
                        assertRewardNotification(r, r.getInitiativeId(), "USERID_INITIATIVEID_CLOSED_ALREADY_EXPIRED_%s", TOMORROW, BigDecimal.valueOf(6));
    //  TODO          case INITIATIVE_ID_NOTIFY_THRESHOLD -> assertRewardNotification(r, r.getInitiativeId(), "USERID_INITIATIVEID_THRESHOLD_%s", null, BigDecimal.valueOf(7));
    //  TODO          case INITIATIVE_ID_NOTIFY_EXHAUSTED -> assertRewardNotification(r, r.getInitiativeId(), "USERID_INITIATIVEID_EXHAUSTED_%s", TOMORROW, BigDecimal.valueOf(8));
                default -> throw new IllegalArgumentException("Unexpected initiativeId: " + r);
            }
        });
    }

    private void checkRewardsNotification(int validTrx) {
        List<RewardsNotification> rewardsNotifications = Objects.requireNonNull(rewardsNotificationRepository.findAll().collectList().block());
        //TODO
    }

}