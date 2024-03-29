package it.gov.pagopa.common.reactive.azure.storage;

import com.azure.core.http.rest.PagedFlux;
import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.*;
import com.azure.storage.blob.options.BlobDownloadToFileOptions;
import com.azure.storage.blob.options.BlobUploadFromFileOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;

@Slf4j
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class AzureBlobClientImpl implements AzureBlobClient {

    private final BlobContainerAsyncClient blobContainerClient;

    protected AzureBlobClientImpl(
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

        createDirectoryIfNotExists(destination);

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

    private static void createDirectoryIfNotExists(Path localFile) {
        Path directory = localFile.getParent();
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot create directory to store downloaded zip %s".formatted(localFile), e);
            }
        }
    }
}
