package it.gov.pagopa.reward.notification.connector.azure.storage;

import com.azure.core.http.rest.PagedFlux;
import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.*;
import com.azure.storage.blob.options.BlobDownloadToFileOptions;
import com.azure.storage.blob.options.BlobUploadFromFileOptions;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Set;

@Slf4j
public abstract class BaseAzureBlobClientImpl implements AzureBlobClient {

    private final BlobContainerAsyncClient blobContainerClient;

    protected BaseAzureBlobClientImpl(
            String storageConnectionString,
            String blobContainerName
    ) {
        this.blobContainerClient = new BlobServiceClientBuilder()
                .connectionString(storageConnectionString)
                .buildAsyncClient()
                .getBlobContainerAsyncClient(blobContainerName);
    }

    @Override
    public Mono<Response<BlockBlobItem>> uploadFile(File file, String destination, String contentType) {
        log.info("Uploading file {} (contentType={}) into azure blob at destination {}", file.getName(), contentType, destination);

        return blobContainerClient.getBlobAsyncClient(destination)
                .uploadFromFileWithResponse(new BlobUploadFromFileOptions(file.getPath()));
    }

    @Override
    public Mono<Response<Boolean>> deleteFile(String destination) {
        log.info("Deleting file {} from azure blob container", destination);

        return blobContainerClient.getBlobAsyncClient(destination)
                .deleteIfExistsWithResponse(DeleteSnapshotsOptionType.INCLUDE, null);
    }

    @Override
    public PagedFlux<BlobItem> listFiles(String path) {
        return blobContainerClient.listBlobsByHierarchy(path);
    }

    @Override
    public Mono<Response<BlobProperties>> downloadFile(String filePath, Path destination) {
        log.info("Downloading file {} from azure blob container", filePath);

        createDirectoryIfNotExists(destination.toAbsolutePath().getParent().toString());

        return blobContainerClient.getBlobAsyncClient(filePath)
                .downloadToFileWithResponse(new BlobDownloadToFileOptions(destination.toString())
                        // override options
                        .setOpenOptions(Set.of(
                                StandardOpenOption.CREATE,
                                StandardOpenOption.TRUNCATE_EXISTING,
                                StandardOpenOption.READ,
                                StandardOpenOption.WRITE))
                )

                .onErrorResume(BlobStorageException.class, e -> e.getStatusCode()==404? Mono.empty() : Mono.error(e));
    }

    private static void createDirectoryIfNotExists(String localFileName) {
        Path directory = Paths.get(localFileName).getParent();
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException e) {
                throw new IllegalStateException("[REWARD_NOTIFICATION_EXPORT_CSV] Cannot create directory to store csv %s".formatted(localFileName), e);
            }
        }
    }
}
