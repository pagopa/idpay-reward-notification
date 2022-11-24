package it.gov.pagopa.reward.notification.enums;

import java.util.Arrays;

public enum RewardOrganizationImportResult {
    OK("OK - ORDINE ESEGUITO", RewardNotificationStatus.COMPLETED_OK),
    KO("KO", RewardNotificationStatus.COMPLETED_KO);

    public final String value;
    public final RewardNotificationStatus notificationStatus;

    RewardOrganizationImportResult(String value, RewardNotificationStatus status){
        this.value=value;
        this.notificationStatus=status;
    }

    public static RewardOrganizationImportResult fromValue(String value){
        return Arrays.stream(values()).filter(e->e.value.equals(value)).findFirst().orElse(null);
    }

    public RewardNotificationStatus toRewardNotificationStatus() {
        return notificationStatus;
    }
}
