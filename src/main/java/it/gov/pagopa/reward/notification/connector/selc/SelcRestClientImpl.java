package it.gov.pagopa.reward.notification.connector.selc;

import it.gov.pagopa.reward.notification.dto.selc.UserResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.Map;


@Service
@Slf4j
public class SelcRestClientImpl implements SelcRestClient {

    private static final String SUBSCRIPTION_KEY_HEADER = "Ocp-Apim-Subscription-Key";
    private static final String UID_HEADER = "x-selfcare-uid";
    private static final String URI = "/institutions/{organizationId}/products/prod-idpay/users";

    private final WebClient webClient;

    public SelcRestClientImpl(@Value("${app.selc.base-url}") String selcBaseUrl,
                              @Value("${app.selc.headers.subscription-key}") String subscriptionKey,
                              @Value("${app.selc.headers.uid}") String selfCareUid,
                              WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.clone()
                .baseUrl(selcBaseUrl)
                .defaultHeaders(h -> h.addAll(headers(subscriptionKey, selfCareUid)))
                .build();
    }

    @Override
    public Flux<UserResource> getInstitutionProductUsers(String organizationId) {
        return webClient.method(HttpMethod.GET)
                .uri(URI, Map.of("organizationId", organizationId))
                .retrieve()
                .bodyToFlux(UserResource.class)

                .onErrorResume(Exception.class, x -> {
                    log.error("[ExternalService][SELC] Error retrieving product users for organization: {}", organizationId, x);
                    return Flux.fromIterable(Collections.emptyList());
                });
    }

    private HttpHeaders headers(String subscriptionKey, String selfCareUid) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(SUBSCRIPTION_KEY_HEADER, subscriptionKey);
        headers.add(UID_HEADER, selfCareUid);
        return headers;
    }
}
