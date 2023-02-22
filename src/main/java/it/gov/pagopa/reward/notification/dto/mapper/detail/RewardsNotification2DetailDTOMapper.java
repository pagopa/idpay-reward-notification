package it.gov.pagopa.reward.notification.dto.mapper.detail;

import it.gov.pagopa.reward.notification.dto.controller.detail.RewardNotificationDetailDTO;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.utils.Utils;
import org.springframework.stereotype.Service;

@Service
public class RewardsNotification2DetailDTOMapper {

    public RewardNotificationDetailDTO apply(RewardsNotification notification) {
        return RewardNotificationDetailDTO.builder()
                .userId(notification.getUserId())
                .iban(notification.getIban())
                .amount(Utils.cents2Eur(notification.getRewardCents()))
                .startDate(notification.getStartDepositDate())
                .endDate(notification.getNotificationDate())
                .status(notification.getStatus())
                .refundType(getRefundType(notification))
                .cro(notification.getCro())
                .transferDate(notification.getExecutionDate())
                .userNotificationDate(notification.getFeedbackElaborationDate().toLocalDate())
                .build();
    }

    private RewardNotificationDetailDTO.RefundDetailType getRefundType(RewardsNotification notification) {
        if (notification.getOrdinaryId() != null) {
            return RewardNotificationDetailDTO.RefundDetailType.REMEDIAL;
        } else {
            return RewardNotificationDetailDTO.RefundDetailType.ORDINARY;
        }
    }
}
