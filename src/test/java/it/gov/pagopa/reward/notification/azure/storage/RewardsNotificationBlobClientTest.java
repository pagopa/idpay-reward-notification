package it.gov.pagopa.reward.notification.azure.storage;

class RewardsNotificationBlobClientTest extends BaseAzureBlobClientTest{
    @Override
    protected AzureBlobClient builtBlobInstance() {
        return new BaseAzureBlobClientImpl("UseDevelopmentStorage=true;", "test");
    }
}
