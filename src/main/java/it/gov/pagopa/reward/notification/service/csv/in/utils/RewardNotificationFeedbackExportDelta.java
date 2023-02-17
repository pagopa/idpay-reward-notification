package it.gov.pagopa.reward.notification.service.csv.in.utils;

import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class RewardNotificationFeedbackExportDelta {
    private RewardOrganizationExport export;
    private long exportInc;
    private long exportIncOk;
    private long exportDeltaReward;

    public static RewardNotificationFeedbackExportDelta add(RewardNotificationFeedbackExportDelta delta1, RewardNotificationFeedbackExportDelta delta2){
        return new RewardNotificationFeedbackExportDelta(delta1.getExport()
                , delta1.getExportInc() + delta2.getExportInc()
                , delta1.getExportIncOk() + delta2.getExportIncOk()
                , delta1.getExportDeltaReward() + delta2.getExportDeltaReward()
        );
    }
}
