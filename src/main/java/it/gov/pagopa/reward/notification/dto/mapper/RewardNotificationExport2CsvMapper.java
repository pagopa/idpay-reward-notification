package it.gov.pagopa.reward.notification.dto.mapper;

import it.gov.pagopa.reward.notification.dto.rest.MerchantDetailDTO;
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

        setCommonFields(reward, out);
        out.setFiscalCode(user.getFiscalCode());
        out.setBeneficiaryName("%s %s".formatted(user.getName(), user.getSurname()));
        out.setIban(reward.getIban());

        return out;
    }

    public RewardNotificationExportCsvDto apply(RewardsNotification reward, MerchantDetailDTO merchant) {
        RewardNotificationExportCsvDto out = new RewardNotificationExportCsvDto();

        setCommonFields(reward, out);
        out.setFiscalCode(merchant.getFiscalCode());
        out.setBeneficiaryName(merchant.getBusinessName());
        out.setIban(merchant.getIban());

        return out;
    }

    private void setCommonFields(RewardsNotification reward, RewardNotificationExportCsvDto out) {
        String depositStartDateStr = DateTimeFormatter.ISO_DATE.format(reward.getStartDepositDate());
        String depositEndDateStr = DateTimeFormatter.ISO_DATE.format(reward.getNotificationDate());

        out.setId(reward.getId());

        out.setProgressiveCode(reward.getProgressive());
        out.setUniqueID(reward.getExternalId());
        out.setAmountCents(reward.getRewardCents());
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
    }

    private String buildPaymentReason(RewardsNotification reward, String depositStartDateStr, String depositEndDateStr) {
        return "Rimborso %s %s %s".formatted(
                reward.getInitiativeName(),
                depositStartDateStr,
                depositEndDateStr
        );
    }

}
