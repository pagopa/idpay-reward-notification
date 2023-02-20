package it.gov.pagopa.reward.notification.enums;

public enum RewardNotificationStatus {
    /** if not yet exported */
    TO_SEND,
    /** if the amount exported was 0 when the reward notification date occurs */
    SKIPPED,
    /** if an error occurred while exporting */
    ERROR,
    /** if exported, but not feedback has been received */
    EXPORTED,
    /** if the organization given OK */
    COMPLETED_OK,
    /** if the organization given a KO */
    COMPLETED_KO
}
