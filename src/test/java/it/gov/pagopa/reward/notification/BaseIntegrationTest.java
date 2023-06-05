package it.gov.pagopa.reward.notification;

import com.azure.core.http.rest.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import it.gov.pagopa.common.kafka.KafkaTestUtilitiesService;
import it.gov.pagopa.common.mongo.MongoTestUtilitiesService;
import it.gov.pagopa.common.stream.StreamsHealthIndicator;
import it.gov.pagopa.common.utils.TestIntegrationUtils;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.notification.connector.azure.storage.RewardsNotificationBlobClient;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.data.util.Pair;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@SpringBootTest
@EmbeddedKafka(topics = {
        "${spring.cloud.stream.bindings.refundRuleConsumer-in-0.destination}",
        "${spring.cloud.stream.bindings.rewardTrxConsumer-in-0.destination}",
        "${spring.cloud.stream.bindings.ibanOutcomeConsumer-in-0.destination}",
        "${spring.cloud.stream.bindings.errors-out-0.destination}",
        "${spring.cloud.stream.bindings.rewardNotificationUploadConsumer-in-0.destination}",
        "${spring.cloud.stream.bindings.rewardNotificationFeedback-out-0.destination}",
}, controlledShutdown = true)
@TestPropertySource(
        properties = {
                // even if enabled into application.yml, spring test will not load it https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing.spring-boot-applications.jmx
                "spring.jmx.enabled=true",

                //region common feature disabled
                "app.csv.export.schedule=-",
                "app.rewards-notification.expired-initiatives.schedule=-",
                "app.csv.tmp-dir=target/tmp",
                "logging.level.it.gov.pagopa.common.kafka.service.ErrorNotifierServiceImpl=WARN",
                "logging.level.it.gov.pagopa.common.reactive.kafka.consumer.BaseKafkaConsumer=WARN",
                "logging.level.it.gov.pagopa.common.reactive.utils.PerformanceLogger=WARN",
                //endregion

                //region pdv
                "app.pdv.retry.delay-millis=5000",
                "app.pdv.retry.max-attempts=3",
                //endregion

                //region kafka brokers
                "logging.level.org.apache.zookeeper=WARN",
                "logging.level.org.apache.kafka=WARN",
                "logging.level.kafka=WARN",
                "logging.level.state.change.logger=WARN",
                "spring.cloud.stream.kafka.binder.configuration.security.protocol=PLAINTEXT",
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.kafka.binder.zkNodes=${spring.embedded.zookeeper.connect}",
                "spring.cloud.stream.binders.kafka-idpay-rule.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.binders.kafka-rewarded-transactions.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.binders.kafka-checkiban-outcome.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.binders.kafka-errors.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.binders.kafka-reward-notification-upload.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.binders.kafka-reward-notification-feedback.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                //endregion

                //region mongodb
                "logging.level.org.mongodb.driver=WARN",
                "logging.level.de.flapdoodle.embed.mongo.spring.autoconfigure=WARN",
                "de.flapdoodle.mongodb.embedded.version=4.0.21",
                //endregion

                //region wiremock
                "logging.level.WireMock=ERROR",
                "app.pdv.base-url=http://localhost:${wiremock.server.port}",
                "app.email-notification.base-url=http://localhost:${wiremock.server.port}",
                "app.selc.base-url=http://localhost:${wiremock.server.port}",
                "app.wallet.base-url=http://localhost:${wiremock.server.port}",
                "app.merchant.base-url=http://localhost:${wiremock.server.port}"
                //endregion
        })
@AutoConfigureDataMongo
@AutoConfigureWireMock(stubs = "classpath:/stub", port = 0)
@AutoConfigureWebTestClient
public abstract class BaseIntegrationTest {

    @Autowired
    protected KafkaTestUtilitiesService kafkaTestUtilitiesService;
    @Autowired
    protected MongoTestUtilitiesService mongoTestUtilitiesService;

    @Autowired
    protected StreamsHealthIndicator streamsHealthIndicator;

    @Autowired
    protected ObjectMapper objectMapper;

    @Value("${spring.cloud.stream.bindings.refundRuleConsumer-in-0.destination}")
    protected String topicInitiative2StoreConsumer;
    @Value("${spring.cloud.stream.bindings.rewardTrxConsumer-in-0.destination}")
    protected String topicRewardResponse;
    @Value("${spring.cloud.stream.bindings.ibanOutcomeConsumer-in-0.destination}")
    protected String topicIbanOutcome;
    @Value("${spring.cloud.stream.bindings.rewardNotificationUploadConsumer-in-0.destination}")
    protected String topicRewardNotificationUpload;
    @Value("${spring.cloud.stream.bindings.rewardNotificationFeedback-out-0.destination}")
    protected String topicRewardNotificationFeedback;
    @Value("${spring.cloud.stream.bindings.errors-out-0.destination}")
    protected String topicErrors;

    @Value("${spring.cloud.stream.bindings.refundRuleConsumer-in-0.group}")
    protected String groupIdInitiative2StoreConsumer;
    @Value("${spring.cloud.stream.bindings.rewardTrxConsumer-in-0.group}")
    protected String groupIdRewardResponse;
    @Value("${spring.cloud.stream.bindings.ibanOutcomeConsumer-in-0.group}")
    protected String groupIdIbanOutcomeConsumer;
    @Value("${spring.cloud.stream.bindings.rewardNotificationUploadConsumer-in-0.group}")
    protected String groupIdRewardNotificationUpload;

    @Autowired
    private WireMockServer wireMockServer;

    @MockBean
    private RewardsNotificationBlobClient rewardsNotificationBlobClientMock;

    @BeforeAll
    public static void unregisterPreviouslyKafkaServers() throws MalformedObjectNameException, MBeanRegistrationException, InstanceNotFoundException {
        TestIntegrationUtils.setDefaultTimeZoneAndUnregisterCommonMBean();
    }

    @PostConstruct
    public void logEmbeddedServerConfig() {
        System.out.printf("""
                        ************************
                        Embedded mongo: %s
                        Embedded kafka: %s
                        Wiremock HTTP: http://localhost:%s
                        Wiremock HTTPS: %s
                        ************************
                        """,
                mongoTestUtilitiesService.getMongoUrl(),
                kafkaTestUtilitiesService.getKafkaUrls(),
                wireMockServer.getOptions().portNumber(),
                wireMockServer.baseUrl());

        mockAzureBlobClient();
    }

    protected void mockAzureBlobClient() {
        Mockito.lenient().when(rewardsNotificationBlobClientMock.uploadFile(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenAnswer(i-> Mono.fromSupplier(() -> {
                    File zipFile = i.getArgument(0);
                    Path zipPath = Path.of(zipFile.getAbsolutePath());
                    Path destination = zipPath.getParent().resolve(zipPath.getFileName().toString().replace(".zip", ".uploaded.zip"));
                    try {
                            Files.copy(zipPath,
                                    destination,
                                    StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            throw new RuntimeException("Something gone wrong simulating upload of test file %s into %s".formatted(zipPath, destination), e);
                        }
                        //noinspection rawtypes
                    Response responseMocked = Mockito.mock(Response.class);
                    Mockito.when(responseMocked.getStatusCode()).thenReturn(201);
                    return responseMocked;
                }));

        Mockito.lenient().when(rewardsNotificationBlobClientMock.downloadFile(Mockito.any(), Mockito.any()))
                .thenAnswer(i-> Mono.fromSupplier(()->{
                    Path zipFile = Path.of("src/test/resources/feedbackUseCasesZip", i.getArgument(0, String.class));
                    Path destination = i.getArgument(1);

                    Path destinationDir = destination.getParent();

                    try {
                        if (!Files.exists(destinationDir)) {
                            Files.createDirectories(destinationDir);
                        }

                        Files.copy(zipFile,
                                destination,
                                StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        throw new RuntimeException("Something gone wrong simulating donwlonad of test file from %s into %s".formatted(zipFile, destination), e);
                    }
                    //noinspection rawtypes
                    Response responseMocked = Mockito.mock(Response.class);
                    Mockito.when(responseMocked.getStatusCode()).thenReturn(206);
                    return responseMocked;
                }));
    }

    @Test
    void testHealthIndicator(){
        Health health = streamsHealthIndicator.health();
        Assertions.assertEquals(Status.UP, health.getStatus());
    }

    protected Pattern getErrorUseCaseIdPatternMatch() {
        return Pattern.compile("\"initiativeId\":\"id_([0-9]+)_?[^\"]*\"");
    }

    protected void checkErrorsPublished(int expectedErrorMessagesNumber, long maxWaitingMs, List<Pair<Supplier<String>, Consumer<ConsumerRecord<String, String>>>> errorUseCases) {
        kafkaTestUtilitiesService.checkErrorsPublished(topicErrors, getErrorUseCaseIdPatternMatch(), expectedErrorMessagesNumber, maxWaitingMs, errorUseCases);
    }

    protected void checkErrorMessageHeaders(String srcTopic,String group, ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload, String expectedKey) {
        kafkaTestUtilitiesService.checkErrorMessageHeaders(srcTopic, group, errorMessage, errorDescription, expectedPayload, expectedKey, this::normalizePayload);
    }

    protected void checkErrorMessageHeaders(String srcTopic,String group, ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload, String expectedKey, boolean expectRetryHeader, boolean expectedAppNameHeader) {
        kafkaTestUtilitiesService.checkErrorMessageHeaders(srcTopic, group, errorMessage, errorDescription, expectedPayload, expectedKey, expectRetryHeader, expectedAppNameHeader, this::normalizePayload);
    }

    protected String normalizePayload(String expectedPayload) {
        String temp = TestUtils.truncateDateTimeField(expectedPayload, "elaborationDateTime");
        temp = TestUtils.setNullFieldValue(temp, "ruleEngineTopicPartition");
        temp = TestUtils.setNullFieldValue(temp, "ruleEngineTopicOffset");
        return TestUtils.truncateDateTimeField(temp,"timestamp");
    }
}
