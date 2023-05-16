package it.gov.pagopa.reward.notification.enums;

public enum RewardOrganizationExportStatus {
    /** if the export is to be processed */
    TO_DO,
    /** if the export is in progress */
    IN_PROGRESS,
    /** if the export is gone into error */
    ERROR,
    /** transient state used just to log a delete export due to no rows */
    SKIPPED,
    /** if the export is completed and the file resulted has been uploaded */
    EXPORTED,
    /** if at least one record has been returned by the organization, but at least one record not */
    PARTIAL,
    /** if all records have been returned by the organization */
    COMPLETE
    }
