package it.gov.pagopa.reward.notification.service.email;

import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.connector.email.EmailNotificationRestClient;
import it.gov.pagopa.reward.notification.connector.selc.SelcRestClient;
import it.gov.pagopa.reward.notification.dto.email.EmailMessageDTO;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationImportStatus;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.repository.RewardNotificationRuleRepository;
import it.gov.pagopa.reward.notification.test.fakers.RewardNotificationRuleFaker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TestPropertySource(
        properties = {
                "app.email-notification.comma-delimiter=,",
                "app.selc.headers.subscription-key=subscriptionKey1",
                "app.selc.headers.uid=selfcareUid1"
        }
)
class EmailNotificationServiceImplTest extends BaseIntegrationTest {

    public static final String INITIATIVEID = "TEST_EMAIL_INITIATIVEID";
    public static final String INITIATIVENAME = "TEST_EMAIL_INITIATIVENAME";
    public static final String ORGANIZATIONID = "TEST_EMAIL_ORGID";
    public static final String TEST_EMAIL_OK = "TEST_EMAIL";
    public static final String TEST_EMAIL_KO = "TEST_EMAIL_KO";
    public static final String FILE_NAME = "testEmail.zip";
    public static final LocalDateTime DATE = LocalDateTime.of(2023, 3, 15, 0, 0);
    @Value("${app.email-notification.comma-delimiter}")
    private String commaDelimiter;
    @SpyBean
    private EmailNotificationRestClient emailRestClientSpy;
    @Autowired
    private SelcRestClient selcRestClient;
    @Autowired
    private RewardNotificationRuleRepository notificationRuleRepository;

    private EmailNotificationService service;

    @BeforeEach
    void setup() {
        service = new EmailNotificationServiceImpl(commaDelimiter, emailRestClientSpy, selcRestClient, notificationRuleRepository);
        prepareTestData();
    }

    @AfterEach
    void clean() {
        notificationRuleRepository.deleteById(INITIATIVEID).block();
    }

    @Test
    void testOk() {

        RewardOrganizationImport expectedImport = buildExpectedImport(false);

        RewardOrganizationImport result = service.send(expectedImport, TEST_EMAIL_OK, TEST_EMAIL_OK).block();

        Assertions.assertEquals(expectedImport, result);

        EmailMessageDTO expectedMessage = buildExpectedMessageDTO(false);
        Mockito.verify(emailRestClientSpy).send(expectedMessage);
    }

    @Test
    void testKo() {

        RewardOrganizationImport expectedImport = buildExpectedImport(true);

        RewardOrganizationImport result = service.send(expectedImport, TEST_EMAIL_KO, TEST_EMAIL_KO).block();

        Assertions.assertEquals(expectedImport, result);

        EmailMessageDTO expectedMessage = buildExpectedMessageDTO(true);
        Mockito.verify(emailRestClientSpy).send(expectedMessage);
    }

    private void prepareTestData() {
        RewardNotificationRule rule = RewardNotificationRuleFaker.mockInstanceBuilder(1)
                .initiativeId(INITIATIVEID)
                .initiativeName(INITIATIVENAME)
                .organizationId(ORGANIZATIONID)
                .build();
        notificationRuleRepository.save(rule).block();
    }

    private RewardOrganizationImport buildExpectedImport(boolean ko) {
        return RewardOrganizationImport.builder()
                .filePath("%s/%s/import/%s".formatted(ORGANIZATIONID, INITIATIVEID, FILE_NAME))
                .initiativeId(INITIATIVEID)
                .organizationId(ko ? "ORGID_NOTFOUND" : ORGANIZATIONID)
                .feedbackDate(DATE)
                .eTag("ETAG")
                .contentLength(1)
                .url("URL")
                .rewardsResulted(1L)
                .elabDate(DATE)
                .status(RewardOrganizationImportStatus.COMPLETE)
                .build();
    }

    private EmailMessageDTO buildExpectedMessageDTO(boolean ko) {
        Map<String, String> templateValues = new HashMap<>();

        templateValues.put("initiativeName", INITIATIVENAME);
        templateValues.put("fileName", FILE_NAME);
        templateValues.put("elabDate", DATE.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

        return EmailMessageDTO.builder()
                .templateName(ko ? TEST_EMAIL_KO : TEST_EMAIL_OK)
                .templateValues(templateValues)
                .subject(ko ? TEST_EMAIL_KO : TEST_EMAIL_OK)
                .senderEmail(null)
                .recipientEmail(ko ? "" : String.join(commaDelimiter, List.of("test.email1@orgId.it", "test.email2@orgId.it")))
                .build();
    }
}