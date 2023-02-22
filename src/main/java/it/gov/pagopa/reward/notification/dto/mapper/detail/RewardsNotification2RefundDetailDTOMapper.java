package it.gov.pagopa.reward.notification.dto.mapper.detail;

import it.gov.pagopa.reward.notification.dto.controller.detail.RefundDetailDTO;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.model.User;
import org.springframework.stereotype.Service;

@Service
public class RewardsNotification2RefundDetailDTOMapper implements BaseDetailMapper{

    public RefundDetailDTO apply(RewardsNotification notification, User user) {
        return RefundDetailDTO.builder()
                .fiscalCode(user.getFiscalCode())
                .iban(notification.getIban())
                .amount(centsToEur(notification.getRewardCents()))
                .startDate(notification.getStartDepositDate())
                .endDate(notification.getNotificationDate())
                .status(notification.getStatus())
                .refundType(getRefundType(notification))
                .cro(notification.getCro())
                .transferDate(notification.getExecutionDate())
                .userNotificationDate(notification.getFeedbackElaborationDate().toLocalDate())
                .build();
    }

    private RefundDetailDTO.RefundDetailType getRefundType(RewardsNotification notification) {
        if (notification.getOrdinaryId() != null) {
            return RefundDetailDTO.RefundDetailType.REMEDIAL;
        } else {
            return RefundDetailDTO.RefundDetailType.ORDINARY;
        }
    }
}
