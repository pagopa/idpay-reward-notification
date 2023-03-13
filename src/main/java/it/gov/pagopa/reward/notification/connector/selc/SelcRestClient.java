package it.gov.pagopa.reward.notification.connector.selc;

import it.gov.pagopa.reward.notification.dto.selc.UserResource;
import reactor.core.publisher.Mono;

import java.util.List;

public interface SelcRestClient {

    Mono<List<UserResource>> getInstitutionProductUsers(String organizationId);
}
