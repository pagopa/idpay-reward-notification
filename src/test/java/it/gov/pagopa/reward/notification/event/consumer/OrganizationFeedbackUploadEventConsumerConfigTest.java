package it.gov.pagopa.reward.notification.event.consumer;

import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationExportStatus;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationImportResult;
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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
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

        List<Pair<String, String>> payloads = new ArrayList<>(IntStream.range(0, messages)
                .mapToObj(StorageEventDtoFaker::mockInstance)
                .map(p->Pair.of("orgId=orgId;initiativeId=initiativeId", "[%s]".formatted(TestUtils.jsonSerializer(p))))
                .toList());
        payloads.addAll(IntStream.range(0, notValidMessages).mapToObj(i -> Pair.<String,String>of(null, errorUseCases.get(i).getKey().get())).toList());

        long timeStart = System.currentTimeMillis();
        payloads.forEach(p -> publishIntoEmbeddedKafka(topicRewardNotificationUpload, null, p.getKey(), p.getValue()));
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
                        .organizationId("orgId")
                        .initiativeId("initiativeId")
                        .initiativeName("initiativeName")
                        .filePath("orgId/initiativeId/export/dispositive-rewards-0.zip")
                        .progressive(0)

                        .rewardNotified(6L)
                        .rewardsExportedCents(10_00L + (3 + 6 + 9 + 12 + 15) * 100L) // simulating already 1 notification of 10 retrieved
                        .notificationDate(LocalDate.now())
                        .exportDate(LocalDate.now())

                        .rewardsResulted(1L)
                        .rewardsResultedOk(1L)
                        .rewardsResultsCents(10_00L)

                        .percentageResulted(16_66L)
                        .percentageResultedOk(16_66L)
                        .percentageResults(18_18L)

                        .build(),

                RewardOrganizationExportsFaker.mockInstanceBuilder(1)
                        .id("exportId1")
                        .organizationId("orgId")
                        .initiativeId("initiativeId")
                        .initiativeName("initiativeName")
                        .filePath("orgId/initiativeId/export/dispositive-rewards-1.zip")
                        .progressive(1)

                        .rewardNotified(6L)
                        .rewardsExportedCents((1 + 4 + 7 + 10 + 13 + 16) * 100L)
                        .notificationDate(LocalDate.now())
                        .exportDate(LocalDate.now())

                        .rewardsResulted(0L)
                        .rewardsResultedOk(0L)
                        .rewardsResultsCents(0L)

                        .percentageResulted(0L)
                        .percentageResultedOk(0L)
                        .percentageResults(0L)

                        .build(),

                RewardOrganizationExportsFaker.mockInstanceBuilder(2)
                        .id("exportId2")
                        .organizationId("orgId")
                        .initiativeId("initiativeId")
                        .initiativeName("initiativeName")
                        .filePath("orgId/initiativeId/export/dispositive-rewards-2.zip")
                        .progressive(2)

                        .rewardNotified(6L)
                        .rewardsExportedCents((2 + 5 + 8 + 11 + 14 + 17) * 100L)
                        .notificationDate(LocalDate.now())
                        .exportDate(LocalDate.now())

                        .rewardsResulted(0L)
                        .rewardsResultedOk(0L)
                        .rewardsResultsCents(0L)

                        .percentageResulted(0L)
                        .percentageResultedOk(0L)
                        .percentageResults(0L)

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
        //region reward-dispositive-0
        {
            RewardOrganizationImport stored0 = rewardOrganizationImportsRepository.findById("orgId/initiativeId/import/reward-dispositive-0.zip").block();
            Assertions.assertNotNull(stored0);
            Assertions.assertTrue(stored0.getFeedbackDate().isAfter(LocalDateTime.now().minusHours(1)));
            Assertions.assertTrue(stored0.getElabDate().isAfter(LocalDateTime.now().minusHours(1)));

            Assertions.assertEquals(
                    RewardOrganizationImport.builder()
                            .filePath("orgId/initiativeId/import/reward-dispositive-0.zip")
                            .organizationId("orgId")
                            .initiativeId("initiativeId")
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
        }
        //endregion

        //region reward-dispositive-1
        {
            RewardOrganizationImport stored1 = rewardOrganizationImportsRepository.findById("orgId/initiativeId/import/reward-dispositive-1.zip").block();
            Assertions.assertNotNull(stored1);
            Assertions.assertTrue(stored1.getFeedbackDate().isAfter(LocalDateTime.now().minusHours(1)));
            Assertions.assertTrue(stored1.getElabDate().isAfter(LocalDateTime.now().minusHours(1)));

            Assertions.assertEquals(
                    RewardOrganizationImport.builder()
                            .filePath("orgId/initiativeId/import/reward-dispositive-1.zip")
                            .organizationId("orgId")
                            .initiativeId("initiativeId")
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
        //endregion
    }

    private void checkRewardOrganizationExports() {
        //region exportId0
        {
            RewardOrganizationExport exportId0 = rewardOrganizationExportsRepository.findById("exportId0").block();
            Assertions.assertNotNull(exportId0);
            Assertions.assertTrue(exportId0.getFeedbackDate().isAfter(LocalDateTime.now().minusHours(1)));

            Assertions.assertEquals(
                    RewardOrganizationExport.builder()
                            .id("exportId0")
                            .initiativeId("initiativeId")
                            .initiativeName("initiativeName")
                            .organizationId("orgId")
                            .filePath("orgId/initiativeId/export/dispositive-rewards-0.zip")
                            .notificationDate(LocalDate.now())
                            .exportDate(LocalDate.now())
                            .progressive(0)

                            .rewardNotified(6L)
                            .rewardsExportedCents(55_00L)

                            .rewardsResulted(6L)
                            .rewardsResultedOk(5L)
                            .rewardsResultsCents(46_00L)

                            .percentageResulted(100_00L)
                            .percentageResultedOk(83_33L)
                            .percentageResults(83_63L)

                            .feedbackDate(exportId0.getFeedbackDate())
                            .status(RewardOrganizationExportStatus.PARTIAL)

                            .build(),
                    exportId0
            );
        }
        //endregion

        //region exportId1
        {
            RewardOrganizationExport exportId1 = rewardOrganizationExportsRepository.findById("exportId1").block();
            Assertions.assertNotNull(exportId1);
            Assertions.assertTrue(exportId1.getFeedbackDate().isAfter(LocalDateTime.now().minusHours(1)));

            Assertions.assertEquals(
                    RewardOrganizationExport.builder()
                            .id("exportId1")
                            .initiativeId("initiativeId")
                            .initiativeName("initiativeName")
                            .organizationId("orgId")
                            .filePath("orgId/initiativeId/export/dispositive-rewards-1.zip")
                            .notificationDate(LocalDate.now())
                            .exportDate(LocalDate.now())
                            .progressive(1)

                            .rewardNotified(6L)
                            .rewardsExportedCents(51_00L)

                            .rewardsResulted(6L)
                            .rewardsResultedOk(4L)
                            .rewardsResultsCents(31_00L)

                            .percentageResulted(100_00L)
                            .percentageResultedOk(66_66L)
                            .percentageResults(60_78L)

                            .feedbackDate(exportId1.getFeedbackDate())
                            .status(RewardOrganizationExportStatus.PARTIAL)

                            .build(),
                    exportId1
            );
        }
        //endregion

        //region exportId2
        {
            RewardOrganizationExport exportId2 = rewardOrganizationExportsRepository.findById("exportId2").block();
            Assertions.assertNotNull(exportId2);
            Assertions.assertTrue(exportId2.getFeedbackDate().isAfter(LocalDateTime.now().minusHours(1)));

            Assertions.assertEquals(
                    RewardOrganizationExport.builder()
                            .id("exportId2")
                            .initiativeId("initiativeId")
                            .initiativeName("initiativeName")
                            .organizationId("orgId")
                            .filePath("orgId/initiativeId/export/dispositive-rewards-2.zip")
                            .notificationDate(LocalDate.now())
                            .exportDate(LocalDate.now())
                            .progressive(2)

                            .rewardNotified(6L)
                            .rewardsExportedCents(57_00L)

                            .rewardsResulted(6L)
                            .rewardsResultedOk(6L)
                            .rewardsResultsCents(57_00L)

                            .percentageResulted(100_00L)
                            .percentageResultedOk(100_00L)
                            .percentageResults(100_00L)

                            .feedbackDate(exportId2.getFeedbackDate())
                            .status(RewardOrganizationExportStatus.COMPLETE)

                            .build(),
                    exportId2
            );
        }
        //endregion
    }

    private void checkRewardNotificationFeedbacks() {
        List<String> ids = testDataRewardsNotifications.stream().map(RewardsNotification::getId).toList();
        List<RewardsNotification> rewards = rewardsNotificationRepository.findAllById(ids).collectList().block();

        Assertions.assertNotNull(rewards);
        Assertions.assertEquals(17, rewards.size(), "Unexpected number of rewards: expected %s, retrieved %s".formatted(ids, rewards.stream().map(RewardsNotification::getId).toList()));

        Set<String> expectedRewardKo = Set.of(
                "rewardNotificationId7",
                "rewardNotificationId9",
                "rewardNotificationId13"
        );
        Map<String, RewardsNotification.RewardNotificationHistory> expected2FeedbackRewards2Previous = Map.of(
                "rewardNotificationId6", RewardsNotification.RewardNotificationHistory.builder().result(RewardOrganizationImportResult.KO).rejectionReason("IBAN NOT VALID").build(),
                "rewardNotificationId7", RewardsNotification.RewardNotificationHistory.builder().result(RewardOrganizationImportResult.OK).build(),
                "rewardNotificationId8",  RewardsNotification.RewardNotificationHistory.builder().result(RewardOrganizationImportResult.OK).build()
        );
        rewards.forEach(r->{
            try {
                Assertions.assertTrue(r.getFeedbackDate().isAfter(LocalDateTime.now().minusHours(1)));

                RewardsNotification.RewardNotificationHistory expectedPreviousOp = expected2FeedbackRewards2Previous.get(r.getId());
                if (!expectedRewardKo.contains(r.getId())) {
                    Assertions.assertEquals(RewardNotificationStatus.COMPLETED_OK, r.getStatus());
                    Assertions.assertEquals(RewardOrganizationImportResult.OK.value, r.getResultCode());
                    Assertions.assertNull(r.getRejectionReason());

                    if ("rewardNotificationId8".equals(r.getId())) {
                        Assertions.assertEquals("qwertyuiopa8-2", r.getCro());
                    } else {
                        Assertions.assertEquals("qwertyuiopa%s".formatted(r.getId().substring(20)), r.getCro());
                    }

                    if (expectedPreviousOp != null) {
                        Assertions.assertEquals(LocalDate.of(2022, 11, 19), r.getExecutionDate());
                    } else {
                        Assertions.assertEquals(LocalDate.of(2022, 11, 18), r.getExecutionDate());
                    }
                } else {
                    Assertions.assertEquals(RewardNotificationStatus.COMPLETED_KO, r.getStatus());
                    Assertions.assertEquals(RewardOrganizationImportResult.KO.value, r.getResultCode());
                    Assertions.assertEquals("IBAN NOT VALID", r.getRejectionReason());
                    Assertions.assertNull(r.getCro());
                    Assertions.assertNull(r.getExecutionDate());
                }

                if (expectedPreviousOp != null) {
                    Assertions.assertEquals(2, r.getFeedbackHistory().size());

                    RewardsNotification.RewardNotificationHistory previousOp = r.getFeedbackHistory().get(0);

                    Assertions.assertTrue(previousOp.getFeedbackDate().isAfter(LocalDateTime.now().minusHours(1)));

                    Assertions.assertEquals("orgId/initiativeId/import/reward-dispositive-0.zip", previousOp.getFeedbackFilePath());
                    Assertions.assertEquals(expectedPreviousOp.getResult(), previousOp.getResult());
                    Assertions.assertEquals(expectedPreviousOp.getRejectionReason(), previousOp.getRejectionReason());
                } else {
                    Assertions.assertEquals(1, r.getFeedbackHistory().size());
                }

                RewardsNotification.RewardNotificationHistory lastNotificiationHistory = r.getFeedbackHistory().get(r.getFeedbackHistory().size() - 1);

                Assertions.assertEquals(RewardOrganizationImportResult.OK.equals(lastNotificiationHistory.getResult()) ? RewardNotificationStatus.COMPLETED_OK : RewardNotificationStatus.COMPLETED_KO, r.getStatus());
                Assertions.assertEquals(lastNotificiationHistory.getResult().value, r.getResultCode());
                Assertions.assertEquals(lastNotificiationHistory.getRejectionReason(), r.getRejectionReason());
                Assertions.assertEquals(lastNotificiationHistory.getFeedbackDate(), r.getFeedbackDate());
            } catch (AssertionError e){
                System.err.printf("Error occurred for reward: %s%n", r);
                throw e;
            }
        });
    }
}
