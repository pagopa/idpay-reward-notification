package it.gov.pagopa.reward.notification.azure.storage;

import com.azure.core.http.rest.PagedFlux;
import com.azure.core.http.rest.Response;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlockBlobItem;
import reactor.core.publisher.Mono;

import java.io.File;

public interface AzureBlobClient {
    Mono<Response<BlockBlobItem>> uploadFile(File file, String destination, String contentType);
    Mono<Response<Boolean>> deleteFile(String destination);
    PagedFlux<BlobItem> listFiles(String path);
}
