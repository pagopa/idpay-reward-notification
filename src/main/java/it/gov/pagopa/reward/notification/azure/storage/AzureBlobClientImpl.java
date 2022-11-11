package it.gov.pagopa.reward.notification.azure.storage;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

@Slf4j
@Service
public class AzureBlobClientImpl implements AzureBlobClient {

    private final String blobContainerName;
    private final CloudBlobClient blobClient;

    public AzureBlobClientImpl(
            @Value("${app.csv.export.storage.connection-string}") String storageConnectionString,
            @Value("${app.csv.export.storage.blob-container-name}") String blobContainerName)
            throws URISyntaxException, InvalidKeyException {
        final CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
        this.blobClient = storageAccount.createCloudBlobClient();
        this.blobContainerName = blobContainerName;
    }

    @Override
    public Mono<File> uploadFile(File file, String destination, String contentType) {
        log.info("Uploading file {} (contentType={}) into azure blob at destination {}", file.getName(), contentType, destination);
        try {
            final CloudBlobContainer blobContainer = blobClient.getContainerReference(blobContainerName);
            final CloudBlockBlob blob = blobContainer.getBlockBlobReference(destination);
            blob.getProperties().setContentType(contentType);
            blob.uploadFromFile(file.getAbsolutePath());
            log.info("File uploaded {} (contentType={}) into azure blob at destination {}", file.getName(), contentType, destination);

        } catch (StorageException | URISyntaxException | IOException e) {
            throw new IllegalStateException(
                    "Cannot upload file file file %s (contentType=%s) into azure blob at destination %s"
                            .formatted(file.getName(), contentType, destination),
                    e);
        }
        return Mono.just(file);
    }
}
