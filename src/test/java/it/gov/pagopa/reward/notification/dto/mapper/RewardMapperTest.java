package it.gov.pagopa.reward.notification.dto.mapper;

import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.enums.RewardStatus;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.Rewards;
import it.gov.pagopa.reward.notification.test.fakers.RewardNotificationRuleFaker;
import it.gov.pagopa.reward.notification.test.fakers.RewardTransactionDTOFaker;
import it.gov.pagopa.reward.notification.test.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RewardMapperTest {
    private final RewardMapper mapper = new RewardMapper();

    @Test
    void testNoRule() {
        // Given
        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(0);
        String initiativeId = "INITIATIVEID";

        // When
        Rewards result = mapper.apply(initiativeId, trx.getRewards().get(initiativeId), trx, null);

        //Then
        Assertions.assertNotNull(result);
        checkCommonFields(result, initiativeId, trx);
        Assertions.assertNull(result.getOrganizationId());
        Assertions.assertEquals(RewardStatus.REJECTED, result.getStatus());

        TestUtils.checkNotNullFields(result, "notificationId", "organizationId");
    }

    @Test
    void testRuleFound() {
        // Given
        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(0);
        String initiativeId = "INITIATIVEID";
        RewardNotificationRule rule = RewardNotificationRuleFaker.mockInstance(0);

        // When
        Rewards result = mapper.apply(initiativeId, trx.getRewards().get(initiativeId), trx, rule);

        //Then
        Assertions.assertNotNull(result);
        checkCommonFields(result, initiativeId, trx);
        Assertions.assertEquals(rule.getOrganizationId(), result.getOrganizationId());
        Assertions.assertEquals(RewardStatus.ACCEPTED, result.getStatus());

        TestUtils.checkNotNullFields(result, "notificationId");
    }

    private void checkCommonFields(Rewards result, String initiativeId, RewardTransactionDTO trx) {
        Assertions.assertEquals("%s_%s".formatted(trx.getId(), initiativeId), result.getId());
        Assertions.assertEquals(trx.getId(), result.getTrxId());
        Assertions.assertEquals(trx.getUserId(), result.getUserId());
        Assertions.assertEquals(initiativeId, result.getInitiativeId());
        Assertions.assertEquals(trx.getOperationTypeTranscoded(), result.getOperationType());
        Assertions.assertEquals(trx.getRewards().get(initiativeId).getAccruedReward(), result.getReward());
    }
}
