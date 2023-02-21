package it.gov.pagopa.reward.notification.dto.mapper.detail;

import it.gov.pagopa.reward.notification.dto.controller.RefundDetailDTO;
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
                // TODO .startDate()
                // TODO .endDate()
                .status(notification.getStatus())
                .refundType(getRefundType(notification))
                .cro(notification.getCro())
                .creationDate(notification.getExportDate())
                // TODO .sendDate()
                // TODO .notificationDate()
                .build();
    }

    private RefundDetailDTO.RefundDetailType getRefundType(RewardsNotification notification) {
        if (notification.getId() != null) { // TODO ordinaryId != null
            return RefundDetailDTO.RefundDetailType.REMEDIAL;
        } else {
            return RefundDetailDTO.RefundDetailType.ORDINARY;
        }
    }
}
