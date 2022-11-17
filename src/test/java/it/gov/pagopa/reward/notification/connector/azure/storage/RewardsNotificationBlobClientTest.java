package it.gov.pagopa.reward.notification.connector.azure.storage;

class RewardsNotificationBlobClientTest extends BaseAzureBlobClientTest{
    @Override
    protected AzureBlobClient builtBlobInstance() {
        return new RewardsNotificationBlobClientImpl("UseDevelopmentStorage=true;", "test");
    }
}
