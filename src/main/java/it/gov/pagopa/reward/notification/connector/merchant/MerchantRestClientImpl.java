package it.gov.pagopa.reward.notification.connector.merchant;

import it.gov.pagopa.reward.notification.dto.merchant.MerchantDetailDTO;
import it.gov.pagopa.reward.notification.exception.ClientExceptionNoBody;
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

    private static final String GET_MERCHANT_URI = "/{initiativeId}/{merchantId}/detail";
    private final WebClient webClient;

    public MerchantRestClientImpl(@Value("${app.merchant.base-url}") String merchantUrl,
                                WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.clone()
                .baseUrl(merchantUrl)
                .build();
    }

    @Override
    public Mono<MerchantDetailDTO> getMerchant(String initiativeId, String merchantId) {
        log.info("[REWARD_NOTIFICATION][MERCHANT_INFO] Fetching details of merchant having id {}", merchantId);

        return webClient.method(HttpMethod.GET)
                .uri(GET_MERCHANT_URI, initiativeId, merchantId)
                .retrieve()
                .toEntity(MerchantDetailDTO.class)
                .map(HttpEntity::getBody)

                .onErrorResume(WebClientResponseException.class, e -> {
                    throw new ClientExceptionNoBody(e.getStatusCode(),
                            "Something gone wrong while invoking merchant service to get detail of merchantId %s on initiative %s"
                                    .formatted(merchantId, initiativeId), true, e);
                });
    }
}
