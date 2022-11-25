package it.gov.pagopa.reward.notification.dto.mapper;

import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.test.fakers.RewardNotificationRuleFaker;
import it.gov.pagopa.reward.notification.test.fakers.RewardTransactionDTOFaker;
import it.gov.pagopa.reward.notification.test.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

class RewardsNotificationMapperTest {

    private final RewardsNotificationMapper mapper = new RewardsNotificationMapper();

    @Test
    void test() {
        // Given
        String notificationId = "NOTIFICATIONID";
        LocalDate notificationDate = LocalDate.now();
        int progressive = 57;
        RewardNotificationRule rule = RewardNotificationRuleFaker.mockInstance(0);
        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(0);

        // When
        RewardsNotification result = mapper.apply(notificationId, notificationDate, progressive, trx, rule);

        // Then
        Assertions.assertNotNull(result);

        checkFields(notificationId, notificationDate, progressive, trx, rule, result);

        TestUtils.checkNotNullFields(result,
                "exportDate",
                "depositType",
                "endDepositDate",
                "sendDate",
                "exportId",
                "iban",
                "checkIbanResult",
                "resultCode",
                "rejectionReason",
                "feedbackDate",
                "feedbackHistory",
                "orderDate",
                "executionDate",
                "trn",
                "cro");
    }

    private void checkFields(String notificationId, LocalDate notificationDate, int progressive, RewardTransactionDTO trx, RewardNotificationRule rule, RewardsNotification result) {
        Assertions.assertEquals(notificationId, result.getId());
        Assertions.assertEquals(rule.getInitiativeId(), result.getInitiativeId());
        Assertions.assertEquals(rule.getInitiativeName(), result.getInitiativeName());
        Assertions.assertEquals(rule.getOrganizationId(), result.getOrganizationId());
        Assertions.assertEquals(rule.getOrganizationFiscalCode(), result.getOrganizationFiscalCode());
        Assertions.assertEquals(trx.getUserId(), result.getUserId());
        Assertions.assertEquals(progressive, result.getProgressive());
        Assertions.assertEquals(LocalDate.now(), result.getStartDepositDate());
        Assertions.assertEquals(notificationDate, result.getNotificationDate());
        Assertions.assertEquals(0L, result.getRewardCents());
        Assertions.assertNotNull(result.getExternalId());
        Assertions.assertEquals(RewardNotificationStatus.TO_SEND, result.getStatus());
    }
}
