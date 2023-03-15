package it.gov.pagopa.reward.notification.service.email;

import it.gov.pagopa.reward.notification.connector.email.EmailNotificationRestClient;
import it.gov.pagopa.reward.notification.connector.selc.SelcRestClient;
import it.gov.pagopa.reward.notification.dto.email.EmailMessageDTO;
import it.gov.pagopa.reward.notification.dto.selc.UserResource;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.repository.RewardNotificationRuleRepository;
import it.gov.pagopa.reward.notification.utils.PerformanceLogger;
import it.gov.pagopa.reward.notification.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class EmailNotificationServiceImpl implements EmailNotificationService{

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private final String commaDelimiter;
    private final EmailNotificationRestClient emailRestClient;
    private final SelcRestClient selcRestClient;
    private final RewardNotificationRuleRepository notificationRuleRepository;

    public EmailNotificationServiceImpl(@Value("${app.email-notification.comma-delimiter}") String commaDelimiter,
                                        EmailNotificationRestClient emailRestClient,
                                        SelcRestClient selcRestClient,
                                        RewardNotificationRuleRepository notificationRuleRepository) {
        this.commaDelimiter = commaDelimiter;
        this.emailRestClient = emailRestClient;
        this.selcRestClient = selcRestClient;
        this.notificationRuleRepository = notificationRuleRepository;
    }

    @Override
    public Mono<RewardOrganizationImport> send(RewardOrganizationImport organizationImport, String templateName, String subject) {

        return PerformanceLogger.logTimingOnNext(
                "FEEDBACK_ELABORATION_NOTIFICATION",
                getInstitutionProductUsers(organizationImport.getOrganizationId())
                        .map(UserResource::getEmail)
                        .collectList()
                        .map(l -> String.join(commaDelimiter, l))
                        .zipWith(notificationRuleRepository.findById(organizationImport.getInitiativeId()))
                        .map(t -> {
                            String initiativeName = t.getT2().getInitiativeName();
                            Map<String, String> templateValues = getTemplateValues(initiativeName, organizationImport);
                            return buildEmailMessage(templateName, templateValues, subject, null, t.getT1());
                        })
                        .flatMap(emailRestClient::send)

                        .onErrorResume(e -> {
                            log.error("Something went wrong sending the email notification", e);
                            return Mono.empty();
                        })

                        .then(Mono.just(organizationImport)),
                imp -> "Sent email notification related to import having id %s and status %s".formatted(imp.getFilePath(), imp.getStatus())
        );
    }

    private Flux<UserResource> getInstitutionProductUsers(String organizationId) {
        return selcRestClient.getInstitutionProductUsers(organizationId);
    }

    private Map<String, String> getTemplateValues(String initiativeName, RewardOrganizationImport organizationImport) {

        Map<String, String> templateValues = new HashMap<>();

        templateValues.put("initiativeName", initiativeName);
        templateValues.put("fileName", Utils.filePath2FileName(organizationImport.getFilePath()));
        templateValues.put("elabDate", organizationImport.getElabDate().format(DATE_TIME_FORMATTER));

        return templateValues;
    }

    private EmailMessageDTO buildEmailMessage(String templateName, Map<String, String> templateValues, String subject, String sender, String recipients) {
        return EmailMessageDTO.builder()
                .templateName(templateName)
                .templateValues(templateValues)
                .subject(subject)
                .senderEmail(sender)
                .recipientEmail(recipients)
                .build();
    }
}
