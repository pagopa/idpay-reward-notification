package it.gov.pagopa.reward.notification.connector.azure.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RewardsNotificationBlobClientImpl extends BaseAzureBlobClientImpl implements RewardsNotificationBlobClient {
    public RewardsNotificationBlobClientImpl(
            @Value("${app.csv.storage.connection-string}") String storageConnectionString,
            @Value("${app.csv.storage.blob-container-name}") String blobContainerName
    ) {
        super(storageConnectionString, blobContainerName);
    }
}
