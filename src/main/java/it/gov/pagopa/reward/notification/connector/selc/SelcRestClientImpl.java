package it.gov.pagopa.reward.notification.connector.selc;

import it.gov.pagopa.reward.notification.dto.selc.UserResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;


@Service
@Slf4j
public class SelcRestClientImpl implements SelcRestClient {

    private static final String URI = "/institutions/{organizationId}/products/prod-idpay/users";

    private final WebClient webClient;

    public SelcRestClientImpl(@Value("/external/v1") String selcBaseUrl,
                              @Value("${rest-client.selc.service.subscriptionKey}") String subscriptionKey,
                              @Value("${rest-client.selc.service.selfCareUid}") String selfCareUid,
                              WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.clone()
                .baseUrl(selcBaseUrl)
                .defaultHeaders(h -> h.addAll(headers(subscriptionKey, selfCareUid)))
                .build();
    }

    @Override
    public Mono<List<UserResource>> getInstitutionProductUsers(String organizationId) {
        return webClient.method(HttpMethod.GET)
                .uri(URI, organizationId)
                .retrieve()
                .bodyToFlux(UserResource.class)
                .collectList()

                .onErrorResume(Exception.class, x -> {
                    log.error("[ExternalService][SELC] Error retrieving product users for organization: {}", organizationId, x);
                    return Mono.just(Collections.emptyList());
                });
    }

    private HttpHeaders headers(String subscriptionKey, String selfCareUid) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Ocp-Apim-Subscription-Key", subscriptionKey);
        headers.add("x-selfcare-uid", selfCareUid);
        return headers;
    }
}
