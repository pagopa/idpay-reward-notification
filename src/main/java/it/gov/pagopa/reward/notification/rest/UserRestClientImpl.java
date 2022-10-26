package it.gov.pagopa.reward.notification.rest;

import it.gov.pagopa.reward.notification.dto.rest.UserInfoPDV;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserRestClientImpl implements UserRestClient {
    private static final String API_KEY_HEADER = "x-api-key";
    private static final String URI = "/tokens/{token}/pii";
    private final int pdvRetryDelay;
    private final WebClient webClient;

    public UserRestClientImpl(@Value("${app.pdv.base-url}") String pdvBaseUrl,
                              @Value("${app.pdv.headers.x-api-key}") String apiKeyValue,
                              @Value("${app.pdv.retry.delay-millis}") int pdvRetryDelay,
                              WebClient.Builder webClientBuilder) {
        this.pdvRetryDelay = pdvRetryDelay;
        this.webClient = webClientBuilder.clone()
                .baseUrl(pdvBaseUrl)
                .defaultHeader(API_KEY_HEADER,apiKeyValue).build();
    }

    @Override
    public Mono<UserInfoPDV> retrieveUserInfo(String userId){

        Map<String, String> params = new HashMap<>();
        params.put("token", userId);

        return  webClient
                .method(HttpMethod.GET)
                .uri(URI, params)
                .retrieve()
                .onStatus(
                        httpStatus -> httpStatus==HttpStatus.NOT_FOUND || httpStatus==HttpStatus.BAD_REQUEST || httpStatus==HttpStatus.INTERNAL_SERVER_ERROR,
                        clientResponse -> {
                            HttpStatus httpStatus = clientResponse.statusCode();
                            return Mono.error(new HttpClientErrorException("An error occurred when call PDV with userId %s: %s".formatted(userId, httpStatus.name()), httpStatus, httpStatus.name(), null, null, null));
                        })//TODO handle 429 HTTP_STATUS retry the invocation (reactive delay)
                .bodyToMono(UserInfoPDV.class);
    }
}
