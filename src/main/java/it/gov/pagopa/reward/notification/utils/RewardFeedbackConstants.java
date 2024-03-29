package it.gov.pagopa.reward.notification.utils;

import it.gov.pagopa.reward.notification.enums.RewardOrganizationImportResult;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class RewardFeedbackConstants {
    private RewardFeedbackConstants(){}

    public static final String AZURE_STORAGE_EVENT_TYPE_BLOB_CREATED = "Microsoft.Storage.BlobCreated";

    public static final String AZURE_STORAGE_SUBJECT_PREFIX = "/blobServices/default/containers/refund/blobs/";

    public enum ImportFileErrors {
        NO_ROWS("Nessuna riga del csv e' stata correttamente elaborata, si prega di verificare la correttezza del contenuto"),
        NO_SIZE("L'archivio zip caricato e' vuoto"),
        EMPTY_ZIP("L'archivio zip caricato e' vuoto"),
        INVALID_CONTENT("L'archivio zip deve contenere un unico file csv avente lo stesso nome dell'archivio"),
        INVALID_CSV_NAME("L'archivio zip deve contenere un unico file csv avente lo stesso nome dell'archivio"),
        INVALID_HEADERS("L'intestazione del csv non e' valida"),

        GENERIC_ERROR("Qualcosa e' andato storto durante l'elaborazione del file");

        public final String description;
        ImportFileErrors(String description){
            this.description=description;
        }
    }

    public enum ImportFeedbackRowErrors {
        INVALID_RESULT("Esito non riconosciuto, gli unici valori ammessi sono :%s".formatted(Arrays.stream(RewardOrganizationImportResult.values()).map(x->"'%s'".formatted(x.value)).collect(Collectors.joining(",")))),
        NOT_FOUND("UniqueId non esistente"),
        CANNOT_UPDATE_RECOVERED_NOTIFICATION("Non e' possibile recepire esiti su una disposizione gia' recuperata da una disposizione correttiva"),
        INVALID_DATE("Il formato della data inserita non e' valido"),
        GENERIC_ERROR("Qualcosa e' andato storto durante l'elaborazione della riga");

        public final String description;
        ImportFeedbackRowErrors(String description){
            this.description=description;
        }
    }
}
