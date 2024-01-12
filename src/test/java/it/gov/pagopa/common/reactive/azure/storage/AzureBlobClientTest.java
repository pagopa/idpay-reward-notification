package it.gov.pagopa.common.reactive.azure.storage;

import com.azure.core.http.HttpResponse;
import com.azure.core.http.rest.PagedFlux;
import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.models.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Stubber;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Set;

class AzureBlobClientTest {

    private AzureBlobClient blobClient;

    @BeforeEach
    void init() {
        blobClient = buildBlobInstance();
    }

    protected AzureBlobClient buildBlobInstance(){
        return new AzureBlobClientImpl("UseDevelopmentStorage=true;", "test");
    }

    @Test
    void test() throws IOException {
        // Given
        File testFile = new File("README.md");
        String destination = "baseAzureBlobClientTest/README.md";
        Path downloadPath = Path.of("target/README.md");
        Files.deleteIfExists(downloadPath.toAbsolutePath());

        BlobContainerAsyncClient mockClient = mockClient(testFile, destination, downloadPath);

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

        // When download
        File downloadedFile = downloadPath.toFile();
        Assertions.assertFalse(downloadedFile.exists());
        Response<BlobProperties> downloadResult = blobClient.downloadFile(destination, downloadPath).block();

        // Then downloadResult
        Assertions.assertNotNull(downloadResult);
        Assertions.assertEquals(206, downloadResult.getStatusCode());
        if (mockClient == null) {
            Assertions.assertTrue(downloadedFile.exists());
            Assertions.assertEquals(testFile.length(), downloadedFile.length());
            Assertions.assertTrue(downloadedFile.delete());
        }

        // When Delete
        Response<Boolean> deleteResult = blobClient.deleteFile(destination).block();

        // Then deleteResult
        Assertions.assertNotNull(deleteResult);
        Assertions.assertTrue(deleteResult.getValue());

        // When List after delete
        if (mockClient != null) {
            mockListFilesOperation(destination, Collections.emptyList(), mockClient);
        }
        List<BlobItem> listAfterDeleteResult = blobClient.listFiles(destination).collectList().block();

        // Then listAfterDeleteResult
        Assertions.assertNotNull(listAfterDeleteResult);
        Assertions.assertEquals(Collections.emptyList(), listAfterDeleteResult);

        // When downloadAfterDeleteResult
        if (mockClient != null) {
            mockDownloadFileOperation(destination, downloadPath, false, mockClient);
        }
        Response<BlobProperties> downloadAfterDeleteResult = blobClient.downloadFile(destination, downloadPath).block();

        // Then downloadResult
        Assertions.assertNull(downloadAfterDeleteResult);
    }

    protected BlobContainerAsyncClient mockClient(File file, String destination, Path downloadPath) {
        try {
            Field clientField = ReflectionUtils.findField(AzureBlobClientImpl.class, "blobContainerClient");
            Assertions.assertNotNull(clientField);
            clientField.setAccessible(true);

            BlobContainerAsyncClient clientMock = Mockito.mock(BlobContainerAsyncClient.class, Mockito.RETURNS_DEEP_STUBS);

            mockUploadOperation(file, destination, clientMock);

            BlobItem mockBlobItem = new BlobItem();
            mockBlobItem.setName(destination);
            mockListFilesOperation(destination, List.of(mockBlobItem), clientMock);

            mockDeleteOperation(destination, clientMock);

            mockDownloadFileOperation(destination, downloadPath, true, clientMock);

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

    private void mockDownloadFileOperation(String destination, Path downloadPath, boolean fileExists, BlobContainerAsyncClient clientMock) {
        BlobAsyncClient blobAsyncClientMock = clientMock.getBlobAsyncClient(destination);

        Stubber stubber;
        if(fileExists){
            @SuppressWarnings("rawtypes") Response responseMock = Mockito.mock(Response.class);
            Mockito.when(responseMock.getStatusCode()).thenReturn(206);

            stubber = Mockito.doReturn(Mono.just(responseMock));
        } else {
            HttpResponse responseMock = Mockito.mock(HttpResponse.class);
            Mockito.when(responseMock.getStatusCode()).thenReturn(404);

            stubber = Mockito.doReturn(Mono.error(new BlobStorageException("NOT FOUND", responseMock, null)));
        }

        stubber
                .when(blobAsyncClientMock)
                .downloadToFileWithResponse(Mockito.argThat(opt ->
                        opt.getFilePath().equals(downloadPath.toString()) && opt.getOpenOptions().equals(Set.of(
                                StandardOpenOption.CREATE,
                                StandardOpenOption.TRUNCATE_EXISTING,
                                StandardOpenOption.READ,
                                StandardOpenOption.WRITE
                        ))));
    }
}
