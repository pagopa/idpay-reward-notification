package it.gov.pagopa.reward.notification.connector.wallet;

import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

public interface WalletRestClient {

    Mono<ResponseEntity<Void>> suspend(String initiativeId, String userId);
}
