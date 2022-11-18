package it.gov.pagopa.reward.notification.utils;

public final class RewardFeedbackConstants {
    private RewardFeedbackConstants(){}

    public static final String AZURE_STORAGE_EVENT_TYPE_BLOB_CREATED = "Microsoft.Storage.BlobCreated";

    public static final String AZURE_STORAGE_SUBJECT_PREFIX = "/blobServices/default/containers/refund/blobs/";

    public enum ImportFileErrors {
        NO_ROWS("Nessuna riga del csv è stata correttamente elaborata, si prega di verificare la correttezza del contenuto"),
        NO_SIZE("L'archivio zip caricato è vuoto"),
        EMPTY_ZIP("L'archivio zip caricato è vuoto"),
        INVALID_CONTENT("L'archivio zip deve contenere un unico file csv avente lo stesso nome dell'archivio"),
        INVALID_CSV_NAME("L'archivio zip deve contenere un unico file csv avente lo stesso nome dell'archivio"),
        INVALID_HEADERS("L'intestazione del csv non è valida"),
        GENERIC_ERROR("Qualcosa è andato storto durante l'elaborazione del file");

        public final String description;
        ImportFileErrors(String description){
            this.description=description;
        }
    }
    //endregion
}
