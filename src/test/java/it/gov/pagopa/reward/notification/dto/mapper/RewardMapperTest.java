package it.gov.pagopa.reward.notification.dto.mapper;

import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.enums.RewardStatus;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.Rewards;
import it.gov.pagopa.reward.notification.test.fakers.RewardNotificationRuleFaker;
import it.gov.pagopa.reward.notification.test.fakers.RewardTransactionDTOFaker;
import it.gov.pagopa.common.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RewardMapperTest {
    private final RewardMapper mapper = new RewardMapper();

    @Test
    void testNoRule() {
        // Given
        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(0);
        String initiativeId = "INITIATIVEID";
        String notificationid = "NOTIFICATIONID";

        // When
        Rewards result = mapper.apply(initiativeId, trx.getRewards().get(initiativeId), trx, null, notificationid);

        //Then
        Assertions.assertNotNull(result);
        checkCommonFields(result, initiativeId, trx, null, notificationid);

        TestUtils.checkNotNullFields(result, "organizationId");
    }

    @Test
    void testNoNotificationId() {
        // Given
        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(0);
        String initiativeId = "INITIATIVEID";
        RewardNotificationRule rule = RewardNotificationRuleFaker.mockInstance(0);

        // When
        Rewards result = mapper.apply(initiativeId, trx.getRewards().get(initiativeId), trx, rule, null);

        //Then
        Assertions.assertNotNull(result);
        checkCommonFields(result, initiativeId, trx, rule, null);

        TestUtils.checkNotNullFields(result, "notificationId");
    }

    @Test
    void testRuleFound() {
        // Given
        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(0);
        String initiativeId = "INITIATIVEID";
        RewardNotificationRule rule = RewardNotificationRuleFaker.mockInstance(0);
        String notificationid = "NOTIFICATIONID";

        // When
        Rewards result = mapper.apply(initiativeId, trx.getRewards().get(initiativeId), trx, rule, notificationid);

        //Then
        Assertions.assertNotNull(result);
        checkCommonFields(result, initiativeId, trx, rule, notificationid);
        TestUtils.checkNotNullFields(result);
    }

    public static void checkCommonFields(Rewards result, String initiativeId, RewardTransactionDTO trx, RewardNotificationRule rule, String notificationId) {
        Assertions.assertEquals("%s_%s".formatted(trx.getId(), initiativeId), result.getId());
        Assertions.assertEquals(trx.getId(), result.getTrxId());
        Assertions.assertEquals(trx.getUserId(), result.getUserId());
        Assertions.assertEquals(initiativeId, result.getInitiativeId());
        Assertions.assertEquals(trx.getOperationTypeTranscoded(), result.getOperationType());
        Assertions.assertEquals(trx.getRewards().get(initiativeId).getAccruedRewardCents(), result.getRewardCents());

        RewardStatus expectedStatus=rule==null || notificationId==null ? RewardStatus.REJECTED : RewardStatus.ACCEPTED;
        Assertions.assertEquals(expectedStatus, result.getStatus());

        Assertions.assertEquals(rule != null? rule.getOrganizationId() : null, result.getOrganizationId());
        Assertions.assertEquals(notificationId, result.getNotificationId());
    }
}
