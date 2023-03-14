package it.gov.pagopa.reward.notification.connector.selc;

import it.gov.pagopa.reward.notification.dto.selc.UserResource;
import reactor.core.publisher.Flux;

public interface SelcRestClient {

    Flux<UserResource> getInstitutionProductUsers(String organizationId);
}
