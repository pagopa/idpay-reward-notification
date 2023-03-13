package it.gov.pagopa.reward.notification.service.email;

import it.gov.pagopa.reward.notification.connector.email.EmailNotificationRestClient;
import it.gov.pagopa.reward.notification.connector.selc.SelcRestClient;
import it.gov.pagopa.reward.notification.dto.selc.UserResource;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;

import java.util.List;

public class EmailNotificationServiceImpl implements EmailNotificationService{

    private final String commaDelimiter;
    private final EmailNotificationRestClient emailRestClient;
    private final SelcRestClient selcRestClient;

    public EmailNotificationServiceImpl(@Value(",") String commaDelimiter,
                                        EmailNotificationRestClient emailRestClient,
                                        SelcRestClient selcRestClient) {
        this.commaDelimiter = commaDelimiter;
        this.emailRestClient = emailRestClient;
        this.selcRestClient = selcRestClient;
    }

    @Override
    public Mono<Void> notifyOrganization() {
        return null;
    }

    private Mono<List<UserResource>> getInstitutionProductUsers(String organizationId) {
        return selcRestClient.getInstitutionProductUsers(organizationId);
    }
}
