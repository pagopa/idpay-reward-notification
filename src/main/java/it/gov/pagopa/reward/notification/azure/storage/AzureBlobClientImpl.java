package it.gov.pagopa.reward.notification.azure.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
public class AzureBlobClientImpl implements AzureBlobClient {
    @Override
    public Mono<File> uploadFile(File file, String destination, String contentType) {// TODO remove this mock and implement it
        log.info("Uploading file {} (contentType{}) into azure blob at destination {}", file.getName(), contentType, destination);
        Path zipPath = Path.of(file.getAbsolutePath());
        try {
            Files.copy(zipPath,
                    zipPath.getParent().resolve(zipPath.getFileName().toString().replace(".zip", ".uploaded.zip")),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return Mono.just(file);
    }
}
