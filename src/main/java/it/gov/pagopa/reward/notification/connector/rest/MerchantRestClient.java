package it.gov.pagopa.reward.notification.connector.rest;

import it.gov.pagopa.reward.notification.dto.rest.MerchantDetailDTO;
import reactor.core.publisher.Mono;

public interface MerchantRestClient {

    Mono<MerchantDetailDTO> getMerchant(String merchantId, String organizationId, String initiativeId);
}
