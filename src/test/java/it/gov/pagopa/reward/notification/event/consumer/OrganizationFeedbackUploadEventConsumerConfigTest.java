package it.gov.pagopa.reward.notification.event.consumer;

import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationImportStatus;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationExportsRepository;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationImportsRepository;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.test.fakers.RewardOrganizationExportsFaker;
import it.gov.pagopa.reward.notification.test.fakers.RewardsNotificationFaker;
import it.gov.pagopa.reward.notification.test.fakers.StorageEventDtoFaker;
import it.gov.pagopa.reward.notification.test.utils.TestUtils;
import it.gov.pagopa.reward.notification.utils.RewardFeedbackConstants;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.reward.notification.service.csv.in.RewardNotificationFeedbackMediatorService=WARN"
})
class OrganizationFeedbackUploadEventConsumerConfigTest extends BaseIntegrationTest {

    @Autowired
    private RewardsNotificationRepository rewardsNotificationRepository;
    @Autowired
    private RewardOrganizationExportsRepository rewardOrganizationExportsRepository;
    @Autowired
    private RewardOrganizationImportsRepository rewardOrganizationImportsRepository;

    private final int messages = 2;
    private final List<String> rewardNotificationImportIds = IntStream.range(0, messages)
            .mapToObj(StorageEventDtoFaker::mockInstance)
            .map(e -> e.getSubject().replace(RewardFeedbackConstants.AZURE_STORAGE_SUBJECT_PREFIX, ""))
            .toList();
    private List<RewardsNotification> testDataRewardsNotifications;
    private List<RewardOrganizationExport> testDataRewardsOrganizationExport;

    @AfterEach
    void clearTestData() {
        if(testDataRewardsNotifications!=null) {
            rewardsNotificationRepository.deleteAll(testDataRewardsNotifications).block();
        }
        if(testDataRewardsOrganizationExport!=null){
            rewardOrganizationExportsRepository.deleteAll(testDataRewardsOrganizationExport).block();
        }
        if(rewardNotificationImportIds!=null){
            rewardOrganizationImportsRepository.deleteAllById(rewardNotificationImportIds).block();
        }
    }

    @Test
    void test() {
        int notValidMessages = errorUseCases.size();

        storeTestData();

        List<String> payloads = new ArrayList<>(IntStream.range(0, messages)
                .mapToObj(StorageEventDtoFaker::mockInstance)
                .map(TestUtils::jsonSerializer)
                .map("[%s]"::formatted)
                .toList());
        payloads.addAll(IntStream.range(0, notValidMessages).mapToObj(i -> errorUseCases.get(i).getFirst().get()).toList());

        long timeStart = System.currentTimeMillis();
        payloads.forEach(p -> publishIntoEmbeddedKafka(topicRewardNotificationUpload, null, null, p));
        long timePublishingEnd = System.currentTimeMillis();

        waitForRewardNotificationFeedbacks();

        long timeEnd = System.currentTimeMillis();

        checkRewardOrganizationImports();
        checkRewardOrganizationExports();
        checkRewardNotificationFeedbacks();

        //TODO check notification

        checkErrorsPublished(notValidMessages, 5000, errorUseCases);

        System.out.printf("""
                        ************************
                        Time spent to send %d (%d + %d) messages (from start): %d millis
                        Time spent to assert reward notification rules stored count (from previous check): %d millis
                        ************************
                        Test Completed in %d millis
                        ************************
                        """,
                messages + notValidMessages,
                messages,
                errorUseCases.size(),
                timePublishingEnd - timeStart,
                timeEnd - timePublishingEnd,
                timeEnd - timeStart
        );

        long timeCommitCheckStart = System.currentTimeMillis();
        Map<TopicPartition, OffsetAndMetadata> srcCommitOffsets = checkCommittedOffsets(topicRewardNotificationUpload, groupIdRewardNotificationUpload, payloads.size());
        long timeCommitCheckEnd = System.currentTimeMillis();

        System.out.printf("""
                        ************************
                        Time occurred to check committed offset: %d millis
                        ************************
                        Source Topic Committed Offsets: %s
                        ************************
                        """,
                timeCommitCheckEnd - timeCommitCheckStart,
                srcCommitOffsets
        );
    }

    private void storeTestData() {
        testDataRewardsNotifications = rewardsNotificationRepository.saveAll(IntStream.rangeClosed(1, 17)
                .mapToObj(i -> RewardsNotificationFaker.mockInstanceBuilder(i)
                        .id("rewardNotificationId%d".formatted(i))
                        .externalId("rewardNotificationExternalId%d".formatted(i))
                        .organizationId("orgId") // TODO add use case
                        .initiativeId("initiativeId") // TODO add use case
                        .exportId("exportId%d".formatted(i % 3))
                        .exportDate(LocalDateTime.now().minusDays(1))
                        .status(RewardNotificationStatus.EXPORTED)
                        .rewardCents(i * 100L)
                        .build())
                .toList()).collectList().block();

        testDataRewardsOrganizationExport = rewardOrganizationExportsRepository.saveAll(List.of(
                RewardOrganizationExportsFaker.mockInstanceBuilder(0)
                        .id("exportId0")
                        .rewardNotified(5L)
                        .rewardsExportedCents((3 + 6 + 9 + 12 + 15) * 100L)
                        .build(),

                RewardOrganizationExportsFaker.mockInstanceBuilder(1)
                        .id("exportId1")
                        .rewardNotified(6L)
                        .rewardsExportedCents((1 + 4 + 7 + 10 + 13 + 16) * 100L)
                        .build(),

                RewardOrganizationExportsFaker.mockInstanceBuilder(2)
                        .id("exportId2")
                        .rewardNotified(6L)
                        .rewardsExportedCents((2 + 5 + 8 + 11 + 14 + 17) * 100L)
                        .build()
        )).collectList().block();
    }

    private void waitForRewardNotificationFeedbacks() {
        Set<RewardOrganizationImportStatus> finalStatuses = Set.of(
                RewardOrganizationImportStatus.COMPLETE,
                RewardOrganizationImportStatus.WARN,
                RewardOrganizationImportStatus.ERROR);
        long[] countSaved = {0};
        //noinspection ConstantConditions
        waitFor(() -> (countSaved[0] = rewardOrganizationImportsRepository.findAllById(rewardNotificationImportIds)
                .filter(i-> finalStatuses.contains(i.getStatus()))
                .count().block()) == messages, () -> "Expected %d saved feedback operations, read %d".formatted(messages, countSaved[0]), 60, 1000);
    }

    //region errorUseCases

    @Override
    protected Pattern getErrorUseCaseIdPatternMatch() {
        return Pattern.compile("\"id\":\"id_([0-9]+)_?[^\"]*\"");
    }
    private final List<Pair<Supplier<String>, Consumer<ConsumerRecord<String, String>>>> errorUseCases = new ArrayList<>();

    {
        String useCaseJsonNotExpected = "{\"id\":\"id_0\",unexpectedStructure:0}";
        errorUseCases.add(Pair.of(
                () -> useCaseJsonNotExpected,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD_NOTIFICATION_FEEDBACK] Unexpected JSON", useCaseJsonNotExpected)
        ));

        String jsonNotValid = "{\"id\":\"id_1\",invalidJson";
        errorUseCases.add(Pair.of(
                () -> jsonNotValid,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD_NOTIFICATION_FEEDBACK] Unexpected JSON", jsonNotValid)
        ));
    }

    private void checkErrorMessageHeaders(ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload) {
        checkErrorMessageHeaders(topicRewardNotificationUpload, groupIdRewardNotificationUpload, errorMessage, errorDescription, expectedPayload, null);
    }

    //endregion

    private void checkRewardOrganizationImports() {
        RewardOrganizationImport stored0 = rewardOrganizationImportsRepository.findById("orgId/initiativeId/import/reward-dispositive-0.zip").block();
        Assertions.assertNotNull(stored0);
        Assertions.assertTrue(stored0.getFeedbackDate().isAfter(LocalDateTime.now().minusHours(1)));
        Assertions.assertTrue(stored0.getElabDate().isAfter(LocalDateTime.now().minusHours(1)));

        Assertions.assertEquals(
                RewardOrganizationImport.builder()
                        .filePath("orgId/initiativeId/import/reward-dispositive-0.zip")
                                .initiativeId("initiativeId")
                        .organizationId("orgId")
                        .feedbackDate(stored0.getFeedbackDate())
                                .eTag("ETAG0")
                        .contentLength(1000)
                        .url("https://STORAGEACCOUNT.blob.core.windows.net/CONTAINERNAME/orgId/initiativeId/import/reward-dispositive-0.zip")

                        .rewardsResulted(17L)
                        .rewardsResultedError(0L)
                        .rewardsResultedOk(14L)
                        .rewardsResultedOkError(0L)

                        .percentageResulted(100_00L)
                        .percentageResultedOk(82_35L)
                        .percentageResultedOkElab(82_35L)
                        .elabDate(stored0.getElabDate())
                        .exportIds(List.of("exportId0", "exportId1", "exportId2"))
                        .status(RewardOrganizationImportStatus.COMPLETE)
                        .errorsSize(0)
                        .errors(Collections.emptyList())
                        .build(),
                stored0
        );

        RewardOrganizationImport stored1 = rewardOrganizationImportsRepository.findById("orgId/initiativeId/import/reward-dispositive-1.zip").block();
        Assertions.assertNotNull(stored1);
        Assertions.assertTrue(stored1.getFeedbackDate().isAfter(LocalDateTime.now().minusHours(1)));
        Assertions.assertTrue(stored1.getElabDate().isAfter(LocalDateTime.now().minusHours(1)));

        Assertions.assertEquals(
                RewardOrganizationImport.builder()
                        .filePath("orgId/initiativeId/import/reward-dispositive-1.zip")
                        .initiativeId("initiativeId")
                        .organizationId("orgId")
                        .feedbackDate(stored1.getFeedbackDate())
                        .eTag("ETAG1")
                        .contentLength(1000)
                        .url("https://STORAGEACCOUNT.blob.core.windows.net/CONTAINERNAME/orgId/initiativeId/import/reward-dispositive-1.zip")

                        .rewardsResulted(3L)
                        .rewardsResultedError(0L)
                        .rewardsResultedOk(2L)
                        .rewardsResultedOkError(0L)

                        .percentageResulted(100_00L)
                        .percentageResultedOk(66_66L)
                        .percentageResultedOkElab(66_66L)
                        .elabDate(stored1.getElabDate())
                        .exportIds(List.of("exportId0", "exportId1", "exportId2"))
                        .status(RewardOrganizationImportStatus.COMPLETE)
                        .errorsSize(0)
                        .errors(Collections.emptyList())
                        .build(),
                stored1
        );
    }

    private void checkRewardOrganizationExports() {
        Assertions.assertEquals(
                RewardOrganizationExport.builder()
                        //id=exportId0, initiativeId=INITIATIVE_ID_0, initiativeName=INITIATIVE_NAME_0_vnj, organizationId=ORGANIZATION_ID_0, filePath=/hpd/0, notificationDate=2001-02-04, exportDate=2001-02-04, progressive=0, rewardsExportedCents=4500, rewardsResultsCents=3600, rewardNotified=5, rewardsResulted=7, rewardsResultedOk=4, percentageResulted=13000, percentageResultedOk=8000, percentageResults=7998, feedbackDate=2022-11-22T20:12:34.871, status=PARTIAL
                        .build(),
                rewardOrganizationExportsRepository.findById("exportId0").block()
        );

        Assertions.assertEquals(
                RewardOrganizationExport.builder()
                        .build(),
                rewardOrganizationExportsRepository.findById("exportId1").block()
        );

        Assertions.assertEquals(
                RewardOrganizationExport.builder()
                        .build(),
                rewardOrganizationExportsRepository.findById("exportId2").block()
        );
    }

    private void checkRewardNotificationFeedbacks() {
        // TODO
    }
}
