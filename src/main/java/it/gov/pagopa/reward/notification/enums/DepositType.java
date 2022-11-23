package it.gov.pagopa.reward.notification.enums;

import lombok.Getter;

public enum DepositType {
    PARTIAL("parziale"),
    FINAL("finale");

    @Getter
    private final String label;
    DepositType(String label){
        this.label=label;
    }
}
