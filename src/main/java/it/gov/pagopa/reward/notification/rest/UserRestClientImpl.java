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
    private final String pdvBaseUrl;
    private final String apiKeyValue;

    public UserRestClientImpl(@Value("${app.pdv.base-url}") String pdvBaseUrl,
                              @Value("${app.pdv.headers.x-api-key}") String apiKeyValue) {
        this.pdvBaseUrl = pdvBaseUrl;
        this.apiKeyValue = apiKeyValue;
    }

    @Override
    public Mono<UserInfoPDV> retrieveUserInfo(String token){
        String apiKeyHeader = "x-api-key";
        String uri = "/tokens/{token}/pii";

        Map<String, String> params = new HashMap<>();
        params.put("token", token);

        return WebClient.create(pdvBaseUrl)
                .method(HttpMethod.GET)
                .uri(uri, params)
                .header(apiKeyHeader, apiKeyValue)
                .retrieve()
                .onStatus(
                        httpStatus -> httpStatus==HttpStatus.NOT_FOUND || httpStatus==HttpStatus.BAD_REQUEST || httpStatus==HttpStatus.INTERNAL_SERVER_ERROR,
                        clientResponse -> {
                            HttpStatus httpStatus = clientResponse.statusCode();
                            return Mono.error(new HttpClientErrorException("An error occurred when call PDV with userId %s: %s".formatted(token, httpStatus.name()), httpStatus, httpStatus.name(), null, null, null));
                        })
              .bodyToMono(UserInfoPDV.class);
    }
}
