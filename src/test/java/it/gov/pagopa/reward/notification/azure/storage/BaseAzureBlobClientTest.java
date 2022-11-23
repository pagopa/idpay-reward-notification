package it.gov.pagopa.reward.notification.azure.storage;

import com.azure.core.http.rest.PagedFlux;
import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlockBlobItem;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Mono;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

abstract class BaseAzureBlobClientTest {

    private AzureBlobClient blobClient;

    @BeforeEach
    void init() {
        blobClient = builtBlobInstance();
    }

    protected abstract AzureBlobClient builtBlobInstance();

    @Test
    protected void test() {
        // Given
        File testFile = new File("README.md");
        String destination = "baseAzureBlobClientTest/README.md";

        BlobContainerAsyncClient mockClient = mockClient(testFile, destination);

        // When Upload
        Response<BlockBlobItem> uploadResult = blobClient.uploadFile(testFile, destination, "text").block();

        // Then uploadResult
        Assertions.assertNotNull(uploadResult);
        Assertions.assertEquals(201, uploadResult.getStatusCode());

        // When List
        List<BlobItem> listResult = blobClient.listFiles(destination).collectList().block();

        // Then listResult
        Assertions.assertNotNull(listResult);
        Assertions.assertEquals(
                List.of("baseAzureBlobClientTest/README.md")
                , listResult.stream().map(BlobItem::getName).toList());

        // When Delete
        Response<Boolean> deleteResult = blobClient.deleteFile(destination).block();

        // Then deleteResult
        Assertions.assertNotNull(deleteResult);
        Assertions.assertTrue(deleteResult.getValue());

        // When List after delete
        if(mockClient != null){
            mockListFilesOperation(destination, Collections.emptyList(), mockClient);
        }
        List<BlobItem> listAfterDeleteResult = blobClient.listFiles(destination).collectList().block();

        // Then listAfterDeleteResult
        Assertions.assertNotNull(listAfterDeleteResult);
        Assertions.assertEquals(Collections.emptyList(), listAfterDeleteResult);
    }

    protected BlobContainerAsyncClient mockClient(File file, String destination) {
        try {
            Field clientField = ReflectionUtils.findField(BaseAzureBlobClientImpl.class, "blobContainerClient");
            Assertions.assertNotNull(clientField);
            clientField.setAccessible(true);

            BlobContainerAsyncClient clientMock = Mockito.mock(BlobContainerAsyncClient.class, Mockito.RETURNS_DEEP_STUBS);

            mockUploadOperation(file, destination, clientMock);

            BlobItem mockBlobItem = new BlobItem();
            mockBlobItem.setName(destination);
            mockListFilesOperation(destination, List.of(mockBlobItem), clientMock);

            mockDeleteOperation(destination, clientMock);

            clientField.set(blobClient, clientMock);

            return clientMock;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void mockUploadOperation(File file, String destination, BlobContainerAsyncClient clientMock) {
        @SuppressWarnings("rawtypes") Response responseMock = Mockito.mock(Response.class);
        Mockito.when(responseMock.getStatusCode()).thenReturn(201);

        //noinspection unchecked
        Mockito.when(clientMock.getBlobAsyncClient(destination)
                        .uploadFromFileWithResponse(Mockito.argThat(
                        opt -> file.getPath().equals(opt.getFilePath())
                )))
                .thenReturn(Mono.just(responseMock));
    }

    private static void mockListFilesOperation(String destination, List<BlobItem> mockedResult, BlobContainerAsyncClient clientMock) {
        @SuppressWarnings("rawtypes") PagedFlux responseMock = Mockito.mock(PagedFlux.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(responseMock.collectList().block()).thenReturn(mockedResult);

        //noinspection unchecked
        Mockito.when(clientMock.listBlobsByHierarchy(destination))
                .thenReturn(responseMock);
    }

    private static void mockDeleteOperation(String destination, BlobContainerAsyncClient clientMock) {
        @SuppressWarnings("rawtypes") Response responseMock = Mockito.mock(Response.class);
        Mockito.when(responseMock.getValue()).thenReturn(true);

        //noinspection unchecked
        Mockito.when(clientMock.getBlobAsyncClient(destination)
                        .deleteIfExistsWithResponse(Mockito.eq(DeleteSnapshotsOptionType.INCLUDE), Mockito.isNull()))
                .thenReturn(Mono.just(responseMock));
    }
}
