package it.gov.pagopa.reward.notification.azure.storage;

import reactor.core.publisher.Mono;

import java.io.File;

public interface AzureBlobClient {
    Mono<Void> uploadFile(File file, String destination, String contentType);
}
