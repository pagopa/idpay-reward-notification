package it.gov.pagopa.reward.notification.enums;

public enum RewardOrganizationImportStatus {
    /** In elaboration in progress */
    IN_PROGRESS,

    /** If the file has not been elaborated */
    ERROR,

    /** If at least the elaboration of one record has gone into error */
    WARN,

    /** When the elaboration completes without errors or warning */
    COMPLETE
    }