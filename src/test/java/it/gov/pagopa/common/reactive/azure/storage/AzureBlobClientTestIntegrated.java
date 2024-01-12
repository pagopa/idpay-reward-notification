package it.gov.pagopa.common.reactive.azure.storage;

import com.azure.storage.blob.BlobContainerAsyncClient;

import java.io.*;
import java.nio.file.Path;
import java.util.Properties;

/**
 * See confluence page: <a href="https://pagopa.atlassian.net/wiki/spaces/IDPAY/pages/615974424/Secrets+UnitTests">Secrets for UnitTests</a>
 */
@SuppressWarnings({"squid:S3577", "NewClassNamingConvention"}) // suppressing class name not match alert: we are not using the Test suffix in order to let not execute this test by default maven configuration because it depends on properties not pushable. See
class AzureBlobClientTestIntegrated extends AzureBlobClientTest {

    private final String connectionString;

    public AzureBlobClientTestIntegrated() throws IOException {
        try(InputStream storageAccountPropertiesIS = new BufferedInputStream(new FileInputStream("src/test/resources/secrets/storageAccount.properties"))){
            Properties props = new Properties();
            props.load(storageAccountPropertiesIS);
            connectionString=props.getProperty("app.csv.storage.connection-string");
        }
    }

    @Override
    protected AzureBlobClient buildBlobInstance() {
        return new AzureBlobClientImpl(connectionString, "refund");
    }

    @Override
    protected BlobContainerAsyncClient mockClient(File file, String destination, Path downloadPath) {
        // Do Nothing
        return null;
    }
}
