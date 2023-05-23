package it.gov.pagopa.reward.notification.utils;

public final class ExportCsvConstants {
    private ExportCsvConstants(){}

    public static final String CTX_KEY_EXPORTED_INITIATIVE_IDS = "EXPORTED_INITIATIVE_IDS";

//region export rejectionReason
    public static final String EXPORT_REJECTION_REASON_IBAN_NOT_FOUND="IBAN_NOT_FOUND";
    public static final String EXPORT_REJECTION_REASON_CF_NOT_FOUND="CF_NOT_FOUND";
    public static final String EXPORT_REJECTION_REASON_MERCHANT_CF_NOT_FOUND="CF_NOT_FOUND";
//endregion
}
