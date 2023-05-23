package it.gov.pagopa.reward.notification.connector.rest;

import it.gov.pagopa.reward.notification.dto.rest.MerchantDetailDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class MerchantRestClientImpl implements MerchantRestClient {

    private static final String GET_MERCHANT_URI = "/{merchantId}/organization/{organizationId}/initiative/{initiativeId}/detail";
    private final WebClient webClient;

    public MerchantRestClientImpl(@Value("${app.merchant.base-url}") String merchantUrl,
                                WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.clone()
                .baseUrl(merchantUrl)
                .build();
    }

    @Override
    public Mono<MerchantDetailDTO> getMerchant(String merchantId, String organizationId, String initiativeId) {
        log.info("[REWARD_NOTIFICATION][MERCHANT_INFO] Fetching details of merchant having id {}", merchantId);

        return webClient.method(HttpMethod.GET)
                .uri(GET_MERCHANT_URI, merchantId, organizationId, initiativeId)
                .retrieve()
                .toEntity(MerchantDetailDTO.class)
                .map(HttpEntity::getBody)

                .onErrorResume(WebClientResponseException.NotFound.class, x -> {
                    log.warn("merchantId not found {} for initiativeId {}", merchantId, initiativeId);
                    return Mono.empty();
                })
                .onErrorResume(WebClientResponseException.BadRequest.class, x -> {
                    log.warn("merchantId not valid: {}", merchantId);
                    return Mono.empty();
                });
    }
}
