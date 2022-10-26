package it.gov.pagopa.reward.notification.dto.mapper;

import it.gov.pagopa.reward.notification.dto.rule.InitiativeRefund2StoreDTO;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.service.utils.Utils;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class Initiative2RewardNotificationRuleMapper implements Function<InitiativeRefund2StoreDTO, RewardNotificationRule> {


    @Override
    public RewardNotificationRule apply(InitiativeRefund2StoreDTO initiativeRefund2StoreDTO) {
        RewardNotificationRule out = RewardNotificationRule.builder()
                .initiativeId(initiativeRefund2StoreDTO.getInitiativeId())
                .initiativeName(initiativeRefund2StoreDTO.getInitiativeName())
                .endDate(initiativeRefund2StoreDTO.getGeneral().getEndDate())
                .organizationId(initiativeRefund2StoreDTO.getOrganizationId())
                .organizationFiscalCode(initiativeRefund2StoreDTO.getOrganizationVat())
                .accumulatedAmount(initiativeRefund2StoreDTO.getRefundRule().getAccumulatedAmount())
                .timeParameter(initiativeRefund2StoreDTO.getRefundRule().getTimeParameter())
                .build();
        if (out.getAccumulatedAmount() != null && out.getAccumulatedAmount().getRefundThreshold() != null) {
            out.getAccumulatedAmount().setRefundThresholdCents(Utils.euro2Cents(out.getAccumulatedAmount().getRefundThreshold()));
        }
        return out;
    }
}
