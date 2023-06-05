package it.gov.pagopa.reward.notification.dto.mapper.detail;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.reward.notification.dto.controller.detail.RewardNotificationDTO;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import org.springframework.stereotype.Service;

@Service
public class RewardsNotification2DTOMapper {

    public RewardNotificationDTO apply(RewardsNotification notification) {
        return RewardNotificationDTO.builder()
                .eventId(notification.getExternalId())
                .iban(notification.getIban())
                .amount(CommonUtilities.centsToEuro(notification.getRewardCents()))
                .status(notification.getStatus())
                .build();
    }
}
