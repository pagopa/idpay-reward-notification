package it.gov.pagopa.reward.notification.utils;

public final class RewardFeedbackConstants {
    private RewardFeedbackConstants(){}

    public static final String AZURE_STORAGE_EVENT_TYPE_BLOB_CREATED = "Microsoft.Storage.BlobCreated";

    public static final String AZURE_STORAGE_SUBJECT_PREFIX = "/blobServices/default/containers/refund/blobs/";
}
