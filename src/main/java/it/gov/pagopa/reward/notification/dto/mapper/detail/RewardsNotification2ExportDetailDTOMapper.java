package it.gov.pagopa.reward.notification.dto.mapper.detail;

import it.gov.pagopa.reward.notification.dto.controller.detail.ExportDetailDTO;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import org.springframework.stereotype.Service;

@Service
public class RewardsNotification2ExportDetailDTOMapper implements BaseDetailMapper {

    public ExportDetailDTO apply(RewardsNotification notification) {
        return ExportDetailDTO.builder()
                .id(notification.getExternalId())
                .iban(notification.getIban())
                .amount(centsToEur(notification.getRewardCents()))
                .status(notification.getStatus())
                .build();
    }
}
