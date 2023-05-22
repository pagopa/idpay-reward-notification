package it.gov.pagopa.reward.notification.connector.merchant;

import it.gov.pagopa.reward.notification.dto.merchant.MerchantDetailDTO;
import reactor.core.publisher.Mono;

public interface MerchantRestClient {

    Mono<MerchantDetailDTO> getMerchant(String initiativeId, String merchantId);
}
