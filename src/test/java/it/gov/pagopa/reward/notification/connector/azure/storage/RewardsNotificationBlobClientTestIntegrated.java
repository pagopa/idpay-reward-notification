package it.gov.pagopa.reward.notification.connector.azure.storage;

import com.azure.storage.blob.BlobContainerAsyncClient;

import java.io.*;
import java.nio.file.Path;
import java.util.Properties;

class RewardsNotificationBlobClientTestIntegrated extends RewardsNotificationBlobClientTest {

    private final String connectionString;

    public RewardsNotificationBlobClientTestIntegrated() throws IOException {
        try(InputStream storageAccountPropertiesIS = new BufferedInputStream(new FileInputStream("src/test/resources/secrets/storageAccount.properties"))){
            Properties props = new Properties();
            props.load(storageAccountPropertiesIS);
            connectionString=props.getProperty("app.csv.storage.connection-string");
        }
    }

    @Override
    protected AzureBlobClient builtBlobInstance() {
        return new RewardsNotificationBlobClientImpl(connectionString, "refund");
    }

    @Override
    protected BlobContainerAsyncClient mockClient(File file, String destination, Path downloadPath) {
        // Do Nothing
        return null;
    }
}
