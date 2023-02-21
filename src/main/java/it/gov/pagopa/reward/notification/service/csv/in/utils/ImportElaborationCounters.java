package it.gov.pagopa.reward.notification.service.csv.in.utils;

import it.gov.pagopa.reward.notification.enums.RewardOrganizationImportResult;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import lombok.*;

import java.util.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class ImportElaborationCounters {
    private Map<String, RewardNotificationFeedbackExportDelta> exportDeltas;

    private long rewardsResulted;
    private long rewardsResultedError;
    private long rewardsResultedOk;
    private long rewardsResultedOkError;

    private List<RewardOrganizationImport.RewardOrganizationImportError> errors = new ArrayList<>();

    public static ImportElaborationCounters add(ImportElaborationCounters c1, ImportElaborationCounters c2) {
        Map<String, RewardNotificationFeedbackExportDelta> exportDeltas = new HashMap<>(c1.getExportDeltas());
        c2.getExportDeltas().forEach((exportId, delta2) -> exportDeltas.compute(exportId, (id, delta1) -> {
            if(delta1!=null){
                return RewardNotificationFeedbackExportDelta.add(delta1, delta2);
            } else {
                return delta2;
            }
        }));

        ArrayList<RewardOrganizationImport.RewardOrganizationImportError> errors = new ArrayList<>(c1.getErrors());
        errors.addAll(c2.getErrors());

        return new ImportElaborationCounters(
                exportDeltas,

                c1.getRewardsResulted() + c2.getRewardsResulted(),
                c1.getRewardsResultedError() + c2.getRewardsResultedError(),
                c1.getRewardsResultedOk() + c2.getRewardsResultedOk(),
                c1.getRewardsResultedOkError() + c2.getRewardsResultedOkError(),
                errors
        );
    }

    public static ImportElaborationCounters fromElaborationResult(RewardNotificationFeedbackHandlerOutcome outcome) {
        boolean isOkOutcome = RewardOrganizationImportResult.OK.equals(outcome.getResult());
        boolean isError = outcome.getError() != null;

        ImportElaborationCounters out = new ImportElaborationCounters();
        out.exportDeltas =outcome.getExportDelta()!=null? Map.of(outcome.getExportDelta().getExport().getId(), outcome.getExportDelta()) : Collections.emptyMap();
        out.rewardsResulted = 1;
        out.rewardsResultedError = isError ? 1 : 0;
        out.rewardsResultedOk = isOkOutcome ? 1 : 0;
        out.rewardsResultedOkError = isOkOutcome && isError ? 1 : 0;
        if(isError){
            out.getErrors().add(outcome.getError());
        }
        return out;
    }
}
