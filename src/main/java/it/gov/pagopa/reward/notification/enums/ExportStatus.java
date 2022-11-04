package it.gov.pagopa.reward.notification.enums;

public enum ExportStatus {
    /** if the export is to be processed */
    TODO,
    /** if the export is in progress */
    IN_PROGRESS,
    /** if the export is completed and the file resulted has been uploaded */
    EXPORTED,
    /** if the exported file has been download at least once */
    READ,
    /** if at least one record has been returned by the organization, but at least one record not */
    PARTIAL,
    /** if all records have been returned by the organization */
    COMPLETE
    }
