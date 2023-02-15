package it.gov.pagopa.reward.notification.enums;

public enum RewardNotificationStatus {
    /** if not yet exported */
    TO_SEND,
    /** if an error occurred while exporting */
    ERROR,
    /** if it was previously an ERROR status and has been recovered */
    RECOVERED,
    /** if exported, but not feedback has been received */
    EXPORTED,
    /** if the organization given OK */
    COMPLETED_OK,
    /** if the organization given a KO */
    COMPLETED_KO
}
