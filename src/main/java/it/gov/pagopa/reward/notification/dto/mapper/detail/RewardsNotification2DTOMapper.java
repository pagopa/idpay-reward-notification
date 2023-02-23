package it.gov.pagopa.reward.notification.dto.mapper.detail;

import it.gov.pagopa.reward.notification.dto.controller.detail.RewardNotificationDTO;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.utils.Utils;
import org.springframework.stereotype.Service;

@Service
public class RewardsNotification2DTOMapper {

    public RewardNotificationDTO apply(RewardsNotification notification) {
        return RewardNotificationDTO.builder()
                .id(notification.getExternalId())
                .iban(notification.getIban())
                .amount(Utils.cents2EurBigDecimal(notification.getRewardCents()))
                .status(notification.getStatus())
                .build();
    }
}
