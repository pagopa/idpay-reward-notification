package it.gov.pagopa.reward.notification.service.csv.in.utils;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class RewardNotificationFeedbackExportDelta {
    private String exportId;
    private long exportInc;
    private long exportIncOk;
    private long exportDeltaReward;
}
