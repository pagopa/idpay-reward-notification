package it.gov.pagopa.reward.notification.service.csv.in.utils;

import it.gov.pagopa.reward.notification.enums.RewardOrganizationImportResult;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RewardNotificationFeedbackHandlerOutcome {
    private RewardOrganizationImportResult result;
    private RewardOrganizationImport.RewardOrganizationImportError error;
}
