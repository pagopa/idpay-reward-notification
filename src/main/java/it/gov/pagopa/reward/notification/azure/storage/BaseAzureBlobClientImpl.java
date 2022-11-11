package it.gov.pagopa.reward.notification.azure.storage;

import com.azure.core.http.rest.PagedFlux;
import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlockBlobItem;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import com.azure.storage.blob.options.BlobUploadFromFileOptions;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.io.File;

@Slf4j
public abstract class BaseAzureBlobClientImpl implements AzureBlobClient {

    private final BlobContainerAsyncClient blobContainerClient;

    protected BaseAzureBlobClientImpl(
            String storageConnectionString,
            String blobContainerName
    ) {
        this.blobContainerClient =  new BlobServiceClientBuilder()
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
                .deleteIfExistsWithResponse(DeleteSnapshotsOptionType.INCLUDE,null);
    }

    @Override
    public PagedFlux<BlobItem> listFiles(String path) {
        return blobContainerClient.listBlobsByHierarchy(path);
    }
}
