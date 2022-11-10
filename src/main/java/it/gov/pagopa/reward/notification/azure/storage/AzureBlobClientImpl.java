package it.gov.pagopa.reward.notification.azure.storage;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.File;

@Service
public class AzureBlobClientImpl implements AzureBlobClient {
    @Override
    public Mono<File> uploadFile(File file, String destination, String contentType) {
        return Mono.just(file);//TODO
    }
}
