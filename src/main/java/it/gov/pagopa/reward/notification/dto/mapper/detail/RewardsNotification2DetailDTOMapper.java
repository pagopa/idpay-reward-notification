package it.gov.pagopa.reward.notification.dto.mapper.detail;

import it.gov.pagopa.reward.notification.dto.controller.detail.RewardNotificationDetailDTO;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.utils.Utils;
import org.springframework.stereotype.Service;


@Service
public class RewardsNotification2DetailDTOMapper {

    public RewardNotificationDetailDTO apply(RewardsNotification notification) {
        return RewardNotificationDetailDTO.builder()
                .id(notification.getId())
                .externalId(notification.getExternalId())
                .userId(notification.getBeneficiaryId())
                .beneficiaryType(String.valueOf(notification.getBeneficiaryType()))
                .merchantFiscalCode(notification.getMerchantFiscalCode())
                .iban(notification.getIban())
                .amountCents(notification.getRewardCents())
                .startDate(notification.getStartDepositDate())
                .endDate(notification.getNotificationDate())
                .status(notification.getStatus())
                .refundType(Utils.getRefundType(notification))
                .cro(notification.getCro())
                .transferDate(notification.getExecutionDate())
                .userNotificationDate(notification.getFeedbackElaborationDate() != null
                        ? notification.getFeedbackElaborationDate().toLocalDate()
                        : null)
                .build();
    }
}
