package it.gov.pagopa.reward.notification.dto.mapper;

import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationExportCsvDto;
import it.gov.pagopa.reward.notification.enums.DepositType;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.model.User;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class RewardNotificationExport2CsvMapper {

    public RewardNotificationExportCsvDto apply(RewardsNotification reward, User user) {
        RewardNotificationExportCsvDto out = new RewardNotificationExportCsvDto();

        String depositStartDateStr = DateTimeFormatter.ISO_DATE.format(reward.getStartDepositDate());
        String depositEndDateStr = DateTimeFormatter.ISO_DATE.format(reward.getNotificationDate());

        out.setId(reward.getId());

        out.setProgressiveCode(reward.getProgressive());
        out.setUniqueID(reward.getExternalId());
        out.setFiscalCode(user.getFiscalCode());
        out.setAccountHolderName(user.getName());
        out.setAccountHolderSurname(user.getSurname());
        out.setIban(reward.getIban());
        out.setAmount(reward.getRewardCents());
        out.setPaymentReason(buildPaymentReason(reward, depositStartDateStr, depositEndDateStr));
        out.setInitiativeName(reward.getInitiativeName());
        out.setInitiativeID(reward.getInitiativeId());
        out.setStartDatePeriod(depositStartDateStr);
        out.setEndDatePeriod(depositEndDateStr);
        out.setOrganizationId(reward.getOrganizationId());
        out.setOrganizationFiscalCode(reward.getOrganizationFiscalCode());
        out.setCheckIban(reward.getCheckIbanResult());
        out.setTypologyReward((reward.getDepositType() == null? DepositType.FINAL : reward.getDepositType()).getLabel());
        out.setRelatedPaymentID(reward.getRecoveredExternalId());

        return out;
    }

    private String buildPaymentReason(RewardsNotification reward, String depositStartDateStr, String depositEndDateStr) {
        return "%s, %s, %s, %s".formatted(
                reward.getId(),
                reward.getInitiativeName(),
                depositStartDateStr,
                depositEndDateStr
        );
    }

}
