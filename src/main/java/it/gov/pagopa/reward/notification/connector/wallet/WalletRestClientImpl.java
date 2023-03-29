package it.gov.pagopa.reward.notification.connector.wallet;

import it.gov.pagopa.reward.notification.exception.ClientExceptionNoBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class WalletRestClientImpl implements WalletRestClient {

    private static final String URI = "/{initiativeId}/{userId}/suspend";
    private final WebClient webClient;

    public WalletRestClientImpl(@Value("${app.wallet.base-url}") String walletUrl,
                                WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.clone()
                .baseUrl(walletUrl)
                .build();
    }

    @Override
    public Mono<ResponseEntity<Void>> suspend(String initiativeId, String userId) {
        log.info("[REWARD_NOTIFICATION][USER_SUSPENSION] Sending suspension of user {} to Wallet", userId);

        return webClient.method(HttpMethod.PUT)
                .uri(URI, initiativeId, userId)
                .retrieve()
                .toBodilessEntity()
                .map(this::validateWalletResponse)
                .onErrorResume(WebClientResponseException.class, e -> {
                    throw new ClientExceptionNoBody(e.getStatusCode());
                });
    }

    private ResponseEntity<Void> validateWalletResponse(ResponseEntity<Void> r) {
        if (r.getStatusCode().is2xxSuccessful()) {
            return r;
        } else {
            throw new ClientExceptionNoBody(r.getStatusCode());
        }
    }
}