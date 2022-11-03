package it.gov.pagopa.reward.notification.dto.mapper;

import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.service.utils.Utils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class RewardsNotificationMapper {
    public RewardsNotification apply(String notificationId, LocalDate notificationDate, long progressive, RewardTransactionDTO trx, RewardNotificationRule rule) {
        return RewardsNotification.builder()
                .id(notificationId)
                .externalId("%s_%s".formatted(UUID.randomUUID(), notificationDate!=null? notificationDate.format(Utils.FORMATTER_DATE) : (notificationId.hashCode() + progressive)))
                .initiativeId(rule.getInitiativeId())
                .initiativeName(rule.getInitiativeName())
                .organizationId(rule.getOrganizationId())
                .organizationFiscalCode(rule.getOrganizationFiscalCode())
                .userId(trx.getUserId())
                .progressive(progressive)
                .startDepositDate(LocalDate.now())
                .notificationDate(notificationDate)
                .rewardCents(0L)
                .status(RewardNotificationStatus.TO_SEND)
                .build();
    }

}
