package it.gov.pagopa.reward.notification.service.email;

import it.gov.pagopa.reward.notification.connector.email.EmailNotificationRestClient;
import it.gov.pagopa.reward.notification.connector.selc.SelcRestClient;
import it.gov.pagopa.reward.notification.dto.email.EmailMessageDTO;
import it.gov.pagopa.reward.notification.dto.selc.UserResource;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.repository.RewardNotificationRuleRepository;
import it.gov.pagopa.common.reactive.utils.PerformanceLogger;
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
    public static final String IMPORTS_ELABORATION_FLOW_NAME = "FEEDBACK_ELABORATION_NOTIFICATION";
    public static final String EXPORT_UPLOAD_FLOW_NAME = "EXPORT_UPLOAD_NOTIFICATION";

    private final String delimiter;
    private final EmailNotificationRestClient emailRestClient;
    private final SelcRestClient selcRestClient;
    private final RewardNotificationRuleRepository notificationRuleRepository;

    private final String importEmailSubject;
    private final String importEmailTemplateName;
    private final String exportEmailSubject;
    private final String exportEmailTemplateName;

    public EmailNotificationServiceImpl(@Value("${app.email-notification.delimiter}") String delimiter,
                                        EmailNotificationRestClient emailRestClient,
                                        SelcRestClient selcRestClient,
                                        RewardNotificationRuleRepository notificationRuleRepository,
                                        @Value("${app.email-notification.imports.subject}") String importEmailSubject,
                                        @Value("${app.email-notification.imports.template-name}") String importEmailTemplateName,
                                        @Value("${app.email-notification.exports.subject}") String exportEmailSubject,
                                        @Value("${app.email-notification.exports.template-name}") String exportEmailTemplateName
    ) {
        this.delimiter = delimiter;
        this.emailRestClient = emailRestClient;
        this.selcRestClient = selcRestClient;
        this.notificationRuleRepository = notificationRuleRepository;
        this.importEmailSubject = importEmailSubject;
        this.importEmailTemplateName = importEmailTemplateName;
        this.exportEmailSubject = exportEmailSubject;
        this.exportEmailTemplateName = exportEmailTemplateName;
    }

    @Override
    public Mono<RewardOrganizationImport> send(RewardOrganizationImport organizationImport) {

        return PerformanceLogger.logTimingOnNext(
                IMPORTS_ELABORATION_FLOW_NAME,
                getInstitutionProductUsers(organizationImport.getOrganizationId())
                        .map(UserResource::getEmail)
                        .collectList()
                        .map(l -> String.join(delimiter, l))
                        .zipWith(notificationRuleRepository.findById(organizationImport.getInitiativeId()))
                        .map(t -> {
                            String initiativeName = t.getT2().getInitiativeName();
                            Map<String, String> templateValues = getTemplateValues(initiativeName, organizationImport);
                            return buildEmailMessage(importEmailTemplateName, templateValues, importEmailSubject, null, t.getT1());
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

    @Override
    public Mono<RewardOrganizationExport> send(RewardOrganizationExport organizationExport) {

        return PerformanceLogger.logTimingOnNext(
                EXPORT_UPLOAD_FLOW_NAME,
                getInstitutionProductUsers(organizationExport.getOrganizationId())
                        .map(UserResource::getEmail)
                        .collectList()
                        .map(l -> String.join(delimiter, l))
                        .zipWith(notificationRuleRepository.findById(organizationExport.getInitiativeId()))
                        .map(t -> {
                            String initiativeName = t.getT2().getInitiativeName();
                            Map<String, String> templateValues = getTemplateValues(initiativeName, organizationExport);
                            return buildEmailMessage(exportEmailTemplateName, templateValues, exportEmailSubject, null, t.getT1());
                        })
                        .flatMap(emailRestClient::send)

                        .onErrorResume(e -> {
                            log.error("Something went wrong sending the email notification", e);
                            return Mono.empty();
                        })

                        .then(Mono.just(organizationExport)),
                exp -> "Sent email notification related to import having id %s and status %s".formatted(exp.getFilePath(), exp.getStatus())
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

    private Map<String, String> getTemplateValues(String initiativeName, RewardOrganizationExport organizationExport) {

        Map<String, String> templateValues = new HashMap<>();

        templateValues.put("initiativeName", initiativeName);
        templateValues.put("fileName", Utils.filePath2FileName(organizationExport.getFilePath()));

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
