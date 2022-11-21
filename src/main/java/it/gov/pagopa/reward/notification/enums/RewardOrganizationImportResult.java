package it.gov.pagopa.reward.notification.enums;

public enum RewardOrganizationImportResult {
    OK("OK - ORDINE ESEGUITO"),
    KO("KO");

    public final String value;
    RewardOrganizationImportResult(String value){
        this.value=value;
    }
}
