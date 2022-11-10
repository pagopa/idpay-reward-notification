package it.gov.pagopa.reward.notification.dto.mapper;

import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationExportCsvDto;
import it.gov.pagopa.reward.notification.enums.DepositType;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.model.User;
import it.gov.pagopa.reward.notification.test.fakers.RewardsNotificationFaker;
import it.gov.pagopa.reward.notification.test.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

class RewardNotificationExport2CsvMapperTest {

    private final RewardNotificationExport2CsvMapper mapper = new RewardNotificationExport2CsvMapper();

    @Test
    void test(){
        RewardsNotification reward = RewardsNotificationFaker.mockInstance(1, "INITIATIVEID", LocalDate.of(2022,1,31));
        reward.setStartDepositDate(LocalDate.of(2022,1,1));
        reward.setIban("IBAN");
        reward.setCheckIbanResult("IBANRESULT");
        reward.setDepositType(DepositType.FINAL);

        User user = User.builder()
                .fiscalCode("CF")
                .name("NAME")
                .surname("SURNAME")
                .build();

        RewardNotificationExportCsvDto result = mapper.apply(reward, user);
        checkResults(reward, result);

        reward.setDepositType(null);
        RewardNotificationExportCsvDto result2 = mapper.apply(reward, user);
        reward.setDepositType(DepositType.FINAL);// default filled if null
        checkResults(reward, result2);
    }

    private void checkResults(RewardsNotification reward, RewardNotificationExportCsvDto result) {
        Assertions.assertNotNull(result);

        Assertions.assertEquals(reward.getProgressive(), result.getProgressiveCode());
        Assertions.assertEquals(reward.getId(), result.getUniqueID());
        Assertions.assertEquals("CF", result.getFiscalCode());
        Assertions.assertEquals("NAME", result.getAccountHolderName());
        Assertions.assertEquals("SURNAME", result.getAccountHolderSurname());
        Assertions.assertEquals(reward.getIban(), result.getIban());
        Assertions.assertEquals(reward.getRewardCents(), result.getAmount());
        Assertions.assertEquals("USERID1_INITIATIVEID_20220131, NAME_1_jmy, 2022-01-01, 2022-01-31", result.getPaymentReason());
        Assertions.assertEquals(reward.getInitiativeName(), result.getInitiativeName());
        Assertions.assertEquals(reward.getInitiativeId(), result.getInitiativeID());
        Assertions.assertEquals("2022-01-01", result.getStartDatePeriod());
        Assertions.assertEquals("2022-01-31", result.getEndDatePeriod());
        Assertions.assertEquals(reward.getOrganizationId(), result.getOrganizationId());
        Assertions.assertEquals(reward.getOrganizationFiscalCode(), result.getOrganizationFiscalCode());
        Assertions.assertEquals(reward.getCheckIbanResult(), result.getCheckIban());
        Assertions.assertEquals("finale", result.getTypologyReward());
        Assertions.assertNull(result.getRelatedPaymentID());

        TestUtils.checkNotNullFields(result, "relatedPaymentID");
    }
}
