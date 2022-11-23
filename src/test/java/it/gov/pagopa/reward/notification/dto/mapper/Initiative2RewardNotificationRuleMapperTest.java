package it.gov.pagopa.reward.notification.dto.mapper;


import it.gov.pagopa.reward.notification.dto.rule.InitiativeRefund2StoreDTO;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.test.fakers.InitiativeRefundDTOFaker;
import it.gov.pagopa.reward.notification.test.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class Initiative2RewardNotificationRuleMapperTest {

    @Test
    void apply() {
        // Given
        Initiative2RewardNotificationRuleMapper initiative2RewardNotificationRuleMapper = new Initiative2RewardNotificationRuleMapper();

        InitiativeRefund2StoreDTO initiativeRefund2StoreDTO = InitiativeRefundDTOFaker.mockInstance(1);

        // When
        RewardNotificationRule result = initiative2RewardNotificationRuleMapper.apply(initiativeRefund2StoreDTO);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(initiativeRefund2StoreDTO.getInitiativeId(), result.getInitiativeId());
        Assertions.assertEquals(initiativeRefund2StoreDTO.getInitiativeName(), result.getInitiativeName());
        Assertions.assertEquals(initiativeRefund2StoreDTO.getGeneral().getEndDate(),result.getEndDate());
        Assertions.assertEquals(initiativeRefund2StoreDTO.getOrganizationId(), result.getOrganizationId());
        Assertions.assertEquals(initiativeRefund2StoreDTO.getRefundRule().getAccumulatedAmount(),result.getAccumulatedAmount());
        Assertions.assertEquals(initiativeRefund2StoreDTO.getRefundRule().getTimeParameter(),result.getTimeParameter());
        Assertions.assertEquals(initiativeRefund2StoreDTO.getOrganizationVat(), result.getOrganizationFiscalCode());

        Assertions.assertEquals(10000L, result.getAccumulatedAmount().getRefundThresholdCents());

        TestUtils.checkNotNullFields(result);
        TestUtils.checkNotNullFields(result.getAccumulatedAmount());
        TestUtils.checkNotNullFields(result.getTimeParameter());
    }
}