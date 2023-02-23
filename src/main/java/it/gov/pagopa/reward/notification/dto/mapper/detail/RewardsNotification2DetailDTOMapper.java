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
                .userId(notification.getUserId())
                .iban(notification.getIban())
                .amount(Utils.cents2EurBigDecimal(notification.getRewardCents()))
                .startDate(notification.getStartDepositDate())
                .endDate(notification.getNotificationDate())
                .status(notification.getStatus())
                .refundType(getRefundType(notification))
                .cro(notification.getCro())
                .transferDate(notification.getExecutionDate())
                .userNotificationDate(notification.getFeedbackElaborationDate().toLocalDate())
                .build();
    }

    private RewardNotificationDetailDTO.RefundType getRefundType(RewardsNotification notification) {
        if (notification.getOrdinaryId() != null) {
            return RewardNotificationDetailDTO.RefundType.REMEDIAL;
        } else {
            return RewardNotificationDetailDTO.RefundType.ORDINARY;
        }
    }
}
