package it.gov.pagopa.reward.notification.service.csv.in.utils;

import it.gov.pagopa.reward.notification.enums.RewardOrganizationImportResult;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImportElaborationCounters {
    private long rewardsResulted;
    private long rewardsResultedError;
    private long rewardsResultedOk;
    private long rewardsResultedOkError;

    private List<RewardOrganizationImport.RewardOrganizationImportError> errors = new ArrayList<>();

    public static ImportElaborationCounters add(ImportElaborationCounters c1, ImportElaborationCounters c2) {
        ArrayList<RewardOrganizationImport.RewardOrganizationImportError> errors = new ArrayList<>(c1.getErrors());
        errors.addAll(c2.getErrors());

        return new ImportElaborationCounters(
                c1.rewardsResulted + c2.rewardsResulted,
                c1.rewardsResultedError + c2.rewardsResultedError,
                c1.rewardsResultedOk + c2.rewardsResultedOk,
                c1.rewardsResultedOkError + c2.rewardsResultedOkError,
                errors
        );
    }

    public static ImportElaborationCounters fromElaborationResult(RewardNotificationFeedbackHandlerOutcome outcome) {
        boolean isOkOutcome = RewardOrganizationImportResult.OK.equals(outcome.getResult());
        boolean isError = outcome.getError() != null;

        ImportElaborationCounters out = new ImportElaborationCounters();
        out.setRewardsResulted(1);
        out.setRewardsResultedError(isError ? 1 : 0);
        out.setRewardsResultedOk(isOkOutcome ? 1 : 0);
        out.setRewardsResultedOkError(isOkOutcome && isError ? 1 : 0);
        if(isError){
            out.getErrors().add(outcome.getError());
        }
        return out;
    }
}
