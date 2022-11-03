package it.gov.pagopa.reward.notification.rest;

import it.gov.pagopa.reward.notification.dto.rest.UserInfoPDV;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserRestClientImpl implements UserRestClient {
    private static final String API_KEY_HEADER = "x-api-key";
    private static final String URI = "/tokens/{token}/pii";
    private final int pdvRetryDelay;
    private final long pdvMaxAttempts;
    private final WebClient webClient;

    public UserRestClientImpl(@Value("${app.pdv.base-url}") String pdvBaseUrl,
                              @Value("${app.pdv.headers.x-api-key}") String apiKeyValue,
                              @Value("${app.pdv.retry.delay-millis}") int pdvRetryDelay,
                              @Value("${app.pdv.retry.max-attempts}") long pdvMaxAttempts,
                              WebClient.Builder webClientBuilder) {
        this.pdvRetryDelay = pdvRetryDelay;
        this.pdvMaxAttempts = pdvMaxAttempts;
        this.webClient = webClientBuilder.clone()
                .baseUrl(pdvBaseUrl)
                .defaultHeader(API_KEY_HEADER,apiKeyValue)
                .build();
    }

    @Override
    public Mono<UserInfoPDV> retrieveUserInfo(String userId){

        Map<String, String> params = new HashMap<>();
        params.put("token", userId);

        return  webClient
                .method(HttpMethod.GET)
                .uri(URI, params)
                .retrieve()
                .bodyToMono(UserInfoPDV.class)
                .retryWhen(Retry.fixedDelay(pdvMaxAttempts,Duration.ofMillis(pdvRetryDelay))
                        .filter(ex -> ex instanceof WebClientResponseException.TooManyRequests)
                );
    }
}
