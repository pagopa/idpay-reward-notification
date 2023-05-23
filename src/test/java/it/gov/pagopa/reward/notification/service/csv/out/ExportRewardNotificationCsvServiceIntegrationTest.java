package it.gov.pagopa.reward.notification.service.csv.out;

import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.dto.mapper.IbanOutcomeDTO2RewardIbanMapper;
import it.gov.pagopa.reward.notification.dto.mapper.RewardFeedbackMapper;
import it.gov.pagopa.reward.notification.dto.rewards.RewardFeedbackDTO;
import it.gov.pagopa.reward.notification.enums.BeneficiaryType;
import it.gov.pagopa.reward.notification.enums.InitiativeRewardType;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationExportStatus;
import it.gov.pagopa.reward.notification.model.*;
import it.gov.pagopa.reward.notification.repository.*;
import it.gov.pagopa.reward.notification.test.fakers.RewardNotificationRuleFaker;
import it.gov.pagopa.reward.notification.test.fakers.RewardsNotificationFaker;
import it.gov.pagopa.reward.notification.utils.ExportCsvConstants;
import it.gov.pagopa.reward.notification.utils.Utils;
import it.gov.pagopa.reward.notification.utils.ZipUtils;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Example;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

@TestPropertySource(properties = {
        "app.csv.export.split-size=500",
        "logging.level.it.gov.pagopa.reward.notification.connector.rest.UserRestClientImpl=WARN",
        "logging.level.it.gov.pagopa.reward.notification.utils.PerformanceLogger=WARN",
})
class ExportRewardNotificationCsvServiceIntegrationTest extends BaseIntegrationTest {

    public static final LocalDate TODAY = LocalDate.now();
    public static final String TODAY_STR = Utils.FORMATTER_DATE.format(TODAY);
    public static final LocalDate YESTERDAY = TODAY.minusDays(1);

    @Value("${app.csv.export.day-before}")
    private int dayBefore;
    @Value("${app.csv.export.split-size}")
    private int splitSize;

    private long N;
    private LocalDate stuckNotificationDate;

    private RewardNotificationRule rule00;
    private RewardNotificationRule rule0;
    private RewardNotificationRule rule1;
    private RewardNotificationRule rule2;
    private RewardNotificationRule rule3;
    private RewardNotificationRule rule4;
    private RewardNotificationRule rule5;
    private RewardNotificationRule rule6;

    @Autowired
    private RewardNotificationRuleRepository ruleRepository;
    @Autowired
    private RewardsNotificationRepository rewardsRepository;
    @Autowired
    private RewardIbanRepository ibanRepository;
    @Autowired
    private RewardOrganizationExportsRepository exportsRepository;
    @Autowired
    private RewardsSuspendedUserRepository suspendedUserRepository;

    @Autowired
    private ExportRewardNotificationCsvService exportRewardNotificationCsvService;
    @Value("${app.csv.tmp-dir}")
    private String csvTmpDir;

    private void storeTestData() {
        N = (long) splitSize * 2 + 8; // use multiple of 4
        stuckNotificationDate = TODAY.minusDays(dayBefore);

        rule0 = ruleRepository.save(RewardNotificationRuleFaker.mockInstanceBuilder(0).initiativeId("INITIATIVEID0").build()).block();
        rule00 = ruleRepository.save(RewardNotificationRuleFaker.mockInstanceBuilder(0).initiativeId("INITIATIVEID00").build()).block();

        rule1 = ruleRepository.save(RewardNotificationRuleFaker.mockInstanceBuilder(1).initiativeId("INITIATIVEID").build()).block();
        rule2 = ruleRepository.save(RewardNotificationRuleFaker.mockInstanceBuilder(2).initiativeId("INITIATIVEID2").build()).block();
        rule3 = ruleRepository.save(RewardNotificationRuleFaker.mockInstanceBuilder(3).initiativeId("INITIATIVEID3").build()).block();
        rule4 = ruleRepository.save(RewardNotificationRuleFaker.mockInstanceBuilder(4).initiativeId("INITIATIVEID4").build()).block();
        rule5 = ruleRepository.save(RewardNotificationRuleFaker.mockInstanceBuilder(5).initiativeId("INITIATIVEID5").build()).block();
        rule6 = ruleRepository.save(RewardNotificationRuleFaker.mockInstanceBuilder(6).initiativeId("INITIATIVEID6")
                .initiativeRewardType(InitiativeRewardType.DISCOUNT).build()).block();

        // useCase rewards to be notified, but without reward amount, initiative without other rewards
        storeRewardsUseCases(1, rule0.getInitiativeId(), YESTERDAY, null, true, true, false, 0L);
        storeRewardsUseCases(1, rule00.getInitiativeId(), YESTERDAY, null, true, true, false, 0L);

        // useCase rewards to be notified, but without reward amount
        storeRewardsUseCases(3, rule1.getInitiativeId(), YESTERDAY, null, true, true, false, 0L);

        // useCase rewards not to be notified
        storeRewardsUseCases(N, rule1.getInitiativeId(), null, null, true, true);

        // useCase rewards to be notified in the future
        storeRewardsUseCases(N, rule1.getInitiativeId(), TODAY.plusDays(1), null, true, true);

        // useCase new rewards to notify
        storeRewardsUseCases(N, rule1.getInitiativeId(), TODAY, null, true, true);

        // useCase rewards partially related to a stuck export
        long baseStuckReward = testCases.get() - 1;
        storeRewardsUseCases(N / 4, rule2.getInitiativeId(), stuckNotificationDate, "STUCKEXPORTID.5", true, true);
        storeRewardsUseCases(N * 3 / 4, rule2.getInitiativeId(), stuckNotificationDate, null, true, true, false, baseStuckReward);

        // useCase rewards to be notified, but without iban
        storeRewardsUseCases(3, rule3.getInitiativeId(), YESTERDAY, null, false, true);

        // useCase rewards to be notified, first split successfully built with the maximum size, no more split due to cf ko
        storeRewardsUseCases(splitSize - 5, rule4.getInitiativeId(), YESTERDAY, null, true, true);
        storeRewardsUseCases(1, rule4.getInitiativeId(), YESTERDAY, null, true, false);
        storeRewardsUseCases(5, rule4.getInitiativeId(), YESTERDAY, null, true, true);

        List<RewardsNotification> suspendedUserNotification = storeRewardsUseCases(1, rule5.getInitiativeId(), YESTERDAY, null, true, true);
        suspendedUserNotification.forEach(n -> suspendedUserRepository.save(new RewardSuspendedUser(n.getBeneficiaryId(), n.getInitiativeId(), n.getOrganizationId())).block());

        // useCase discount initiative (merchant to be refunded) - OK
        storeRewardsUseCases(1, rule6.getInitiativeId(), YESTERDAY, null, false, true, true);
        // useCase discount initiative (merchant to be refunded) - KO
        storeRewardsUseCases(1, rule6.getInitiativeId(), YESTERDAY, null, false, false, true);

        Assertions.assertEquals(testCases.get(), rewardsRepository.count().block());

        // stuck exports

        // storing twice useCase without rewards in order to test the behavior when both executions start with a stuck execution without rewards (we are launching twice the execute method in order to test parallel execution)
        storeStuckExport("STUCKEXPORTIDWithoutRewards.1", 1L, "rewards/notifications/ORGANIZATION_ID_0_hpd/INITIATIVEID0/export/STUCKEXPORTIDWithoutRewards.1.zip", rule0, stuckNotificationDate);
        storeStuckExport("STUCKEXPORTIDWithoutRewards2.1", 1L, "rewards/notifications/ORGANIZATION_ID_0_hpd/INITIATIVEID00/export/STUCKEXPORTIDWithoutRewards2.1.zip", rule00, YESTERDAY);

        storeStuckExport("STUCKEXPORTID.5", 5L, "rewards/notifications/ORGANIZATION_ID_2_izn/INITIATIVEID2/export/STUCKEXPORT.5.zip", rule2, stuckNotificationDate);
    }

    private void storeStuckExport(String id, long progressive, String filePath, RewardNotificationRule rule, LocalDate notificationDate) {
        exportsRepository.save(RewardOrganizationExport.builder()
                .id(id)
                .progressive(progressive)
                .filePath(filePath)
                .initiativeId(rule.getInitiativeId())
                .initiativeName(rule.getInitiativeName())
                .organizationId(rule.getOrganizationId())
                .notificationDate(notificationDate)
                .exportDate(notificationDate)
                .rewardsExportedCents(0L)
                .rewardsResultsCents(0L)
                .rewardNotified(0L)
                .rewardsResulted(0L)
                .rewardsResultedOk(0L)
                .percentageResulted(0L)
                .percentageResultedOk(0L)
                .percentageResults(0L)
                .status(RewardOrganizationExportStatus.IN_PROGRESS)
                .build()).block();
    }

    private final AtomicLong testCases = new AtomicLong(0L);

    private List<RewardsNotification> storeRewardsUseCases(long number, String initiativeId, LocalDate notificationDate, String exportId, boolean hasIban, boolean hasCf) {
        return storeRewardsUseCases(number, initiativeId, notificationDate, exportId, hasIban, hasCf, false, null);
    }

    private List<RewardsNotification> storeRewardsUseCases(long number, String initiativeId, LocalDate notificationDate, String exportId, boolean hasIban, boolean hasCf, boolean isDiscount) {
        return storeRewardsUseCases(number, initiativeId, notificationDate, exportId, hasIban, hasCf, isDiscount, null);
    }

    private List<RewardsNotification> storeRewardsUseCases(long number, String initiativeId, LocalDate notificationDate, String exportId, boolean hasIban, boolean hasCf, boolean isDiscount, Long baseReward) {
        return storeRewardsUseCases(number, initiativeId, notificationDate, exportId, hasIban, hasCf, isDiscount, baseReward, null);
    }

    private List<RewardsNotification> storeRewardsUseCases(long number, String initiativeId, LocalDate notificationDate, String exportId, boolean hasIban, boolean hasCf, boolean isDiscount, Long baseReward, Long rewardCents) {
        long baseId = testCases.getAndAdd(number);
        long baseR = ObjectUtils.firstNonNull(baseReward, baseId - 1);
        return rewardsRepository.saveAll(LongStream.range(baseId, baseId + number).mapToObj(bias -> {
                    RewardsNotification reward =
                            RewardsNotificationFaker.mockInstanceBuilder((int) bias, initiativeId, notificationDate)
                            .beneficiaryId(buildBeneficiaryIdFromUseCase(hasCf, isDiscount, bias))
                            .beneficiaryType(isDiscount ? BeneficiaryType.MERCHANT : BeneficiaryType.CITIZEN)
                            .initiativeId(initiativeId)
                            .rewardCents(bias - baseR)
                            .notificationDate(notificationDate)
                            .exportId(exportId)
                            .build();

                    reward.setId(replaceId(reward.getId(), hasCf, isDiscount));

                    if (rewardCents != null) {
                        reward.setRewardCents(rewardCents);
                    }

                    if (hasIban) {
                        ibanRepository.save(RewardIban.builder()
                                .id(IbanOutcomeDTO2RewardIbanMapper.buildId(reward))
                                .userId(reward.getBeneficiaryId())
                                .initiativeId(initiativeId)
                                .iban("IBAN_%s".formatted(reward.getBeneficiaryId()))
                                .checkIbanOutcome("CHECKIBAN_OUTCOME_%s".formatted(reward.getBeneficiaryId()))
                                .build()).block();
                    }

                    return reward;
                }
        ).toList()).collectList().block();
    }

    @AfterEach
    void clearData() {
        ruleRepository.deleteAll().block();
        rewardsRepository.deleteAll().block();
        ibanRepository.deleteAll().block();
        exportsRepository.deleteAll().block();
        suspendedUserRepository.deleteAll().block();
    }

    @Test
    void test() throws ExecutionException, InterruptedException {
        // Given
        storeTestData();

        // When
        //Calling twice in order to test parallel execution
        CompletableFuture<List<List<RewardOrganizationExport>>> execute1 = exportRewardNotificationCsvService.execute(TODAY).collectList().toFuture();
        CompletableFuture<List<List<RewardOrganizationExport>>> execute2 = exportRewardNotificationCsvService.execute(TODAY).collectList().toFuture();

        List<RewardOrganizationExport> result = Stream.concat(execute1.get().stream().flatMap(List::stream), execute2.get().stream().flatMap(List::stream))
                .sorted(Comparator.comparing(RewardOrganizationExport::getId))
                .toList();

        // Then
        Assertions.assertNotNull(result);

        List<RewardOrganizationExport> successfulExports = result.stream().filter(e -> RewardOrganizationExportStatus.EXPORTED.equals(e.getStatus())).toList();

        Assertions.assertEquals(Set.of(
                        // 3 split for INITIATIVEID
                        "INITIATIVEID_%s.1".formatted(TODAY_STR),
                        "INITIATIVEID_%s.2".formatted(TODAY_STR),
                        "INITIATIVEID_%s.3".formatted(TODAY_STR),

                        // 3 split for INITIATIVEID2, all related to a previous stuck export
                        "STUCKEXPORTID.5",
                        "STUCKEXPORTID.6",
                        "STUCKEXPORTID.7",

                        // 1 for INITIATIVEID4
                        "INITIATIVEID4_%s.1".formatted(TODAY_STR)
                ),
                successfulExports.stream().map(RewardOrganizationExport::getId).collect(Collectors.toSet()),
                "Unexpected exported files size: %s".formatted(result));

        Assertions.assertEquals(Set.of(
                        // stuck rules 0 and 00
                        "STUCKEXPORTIDWithoutRewards.1",
                        "STUCKEXPORTIDWithoutRewards2.1",

                        // rule 0, due to reward 0 and notification date != stuck export notification date
                        "INITIATIVEID0_%s.1".formatted(TODAY_STR),

                        // rule 3 because made of just IBANKO
                        "INITIATIVEID3_%s.1".formatted(TODAY_STR),

                        // rule 5 because suspended
                        "INITIATIVEID5_%s.1".formatted(TODAY_STR)
                ),
                result.stream().filter(e -> RewardOrganizationExportStatus.SKIPPED.equals(e.getStatus())).map(RewardOrganizationExport::getId).collect(Collectors.toSet()),
                "Unexpected result size: %s".formatted(result));

        Assertions.assertEquals(Collections.emptyList(), rewardsRepository.findInitiatives2Notify(Collections.emptyList(), TODAY).collectList().block(), "There are still initiative to be notified!");

        List<RewardOrganizationExport> exports = exportsRepository.findAll()
                .sort(Comparator.comparing(RewardOrganizationExport::getId))
                .collectList().block();

        Assertions.assertEquals(exports, successfulExports);

        checkInitiativeExports("INITIATIVEID_%s.1".formatted(TODAY_STR), rule1, "rewards/notifications/ORGANIZATION_ID_1_uww/INITIATIVEID/export/NAME_1_jmy_%s.1.zip".formatted(TODAY_STR), 1L, TODAY, result);
        checkInitiativeExports("STUCKEXPORTID.5", rule2, "rewards/notifications/ORGANIZATION_ID_2_izn/INITIATIVEID2/export/STUCKEXPORT.5.zip", 5L, stuckNotificationDate, result);

        checkIbanKoUseCases();
        checkCfKoUseCases(result);
        checkSuspendedUseCases();
        checkNoRewardedUseCases();

        checkKoNotification();
    }

    private void checkInitiativeExports(String expectedBaseExportId, RewardNotificationRule rule, String expectedBaseFilePath, long expectedBaseProgressive, LocalDate expectedNotificationDate, List<RewardOrganizationExport> result) {
        // base export
        checkExport(result,
                expectedBaseExportId, rule,
                expectedBaseProgressive, expectedBaseFilePath, expectedNotificationDate, splitSize);
        // split 1
        checkExportSplit(result, 1, splitSize,
                expectedBaseExportId, rule,
                expectedBaseProgressive, expectedBaseFilePath, expectedNotificationDate);
        // split 2
        checkExportSplit(result, 2, (int) (N % splitSize),
                expectedBaseExportId, rule,
                expectedBaseProgressive, expectedBaseFilePath, expectedNotificationDate);

        Assertions.assertEquals(N * (N + 1) / 2,
                result.stream().filter(e -> rule.getInitiativeId().equals(e.getInitiativeId())).mapToLong(RewardOrganizationExport::getRewardsExportedCents).sum()
        );
    }

    private final Pattern csvUniqueIdGroupMatch = Pattern.compile("\"[^\"]*\";\"([^\"]*)\".*");

    @SneakyThrows
    private void checkExport(List<RewardOrganizationExport> result, String exportId, RewardNotificationRule rule, long expectedProgressive, String expectedFilePath, LocalDate expectedNotificationDate, long expectedExportSize) {
        RewardOrganizationExport export = result.stream().filter(e -> exportId.equals(e.getId())).findFirst().orElse(null);
        Assertions.assertNotNull(export);

        Assertions.assertEquals(rule.getInitiativeId(), export.getInitiativeId());
        Assertions.assertEquals(rule.getInitiativeName(), export.getInitiativeName());
        Assertions.assertEquals(rule.getOrganizationId(), export.getOrganizationId());
        Assertions.assertEquals(expectedFilePath, export.getFilePath());
        Assertions.assertEquals(expectedNotificationDate, export.getNotificationDate());
        Assertions.assertEquals(TODAY, export.getExportDate());
        Assertions.assertEquals(expectedProgressive, export.getProgressive());
        Assertions.assertNotNull(export.getRewardsExportedCents());
        Assertions.assertEquals(expectedExportSize, export.getRewardNotified());
        Assertions.assertEquals(0L, export.getRewardsResultsCents());
        Assertions.assertEquals(0L, export.getRewardsResulted());
        Assertions.assertEquals(0L, export.getRewardsResultedOk());
        Assertions.assertEquals(0L, export.getPercentageResulted());
        Assertions.assertEquals(0L, export.getPercentageResultedOk());
        Assertions.assertEquals(0L, export.getPercentageResults());
        Assertions.assertNull(export.getFeedbackDate());
        Assertions.assertEquals(RewardOrganizationExportStatus.EXPORTED, export.getStatus());

        List<RewardsNotification> rewards = rewardsRepository.findExportRewards(exportId).collectList().block();
        Assertions.assertNotNull(rewards);
        Assertions.assertEquals(expectedExportSize, rewards.size());
        rewards.forEach(r -> {
            Assertions.assertEquals("IBAN_%s".formatted(r.getBeneficiaryId()), r.getIban());
            Assertions.assertEquals("CHECKIBAN_OUTCOME_%s".formatted(r.getBeneficiaryId()), r.getCheckIbanResult());
            Assertions.assertEquals(RewardNotificationStatus.EXPORTED, r.getStatus());
            Assertions.assertEquals(LocalDate.now(), r.getExportDate().toLocalDate());
        });

        Path originalZipFile = Paths.get(csvTmpDir, export.getFilePath());
        Assertions.assertFalse(Files.exists(originalZipFile));

        Path uploadedZipPath = Paths.get(originalZipFile.toString().replace(".zip", ".uploaded.zip"));
        Assertions.assertTrue(Files.exists(uploadedZipPath));
        Path csvPath = Paths.get(uploadedZipPath.toString().replace(".uploaded.zip", ".csv"));
        Assertions.assertFalse(Files.exists(csvPath));

        ZipUtils.unzip(uploadedZipPath.toString(), csvPath.getParent().toString());

        try {
            Assertions.assertTrue(Files.exists(csvPath));

            try (Stream<String> lines = Files.lines(csvPath)) {
                Assertions.assertEquals(
                        rewards.stream().map(RewardsNotification::getExternalId).sorted().toList(),
                        lines.skip(1).map(l -> csvUniqueIdGroupMatch.matcher(l).replaceAll("$1")).sorted().toList()
                );
            }
        } finally {
            Files.delete(csvPath);
        }
    }

    private void checkExportSplit(List<RewardOrganizationExport> result, int splitNumber, int splitSize, String expectedBaseExportId, RewardNotificationRule rule, long expectedBaseProgressive, String expectedBaseFilePath, LocalDate expectedNotificationDate) {
        checkExport(result,
                expectedBaseExportId.replaceFirst("\\.%d$".formatted(expectedBaseProgressive), ".%d".formatted(expectedBaseProgressive + splitNumber)),
                rule,
                expectedBaseProgressive + splitNumber,
                expectedBaseFilePath.replaceFirst("\\.%d.zip$".formatted(expectedBaseProgressive), ".%d.zip".formatted(expectedBaseProgressive + splitNumber)),
                expectedNotificationDate, splitSize);
    }

    private void checkIbanKoUseCases() {
        List<RewardsNotification> expectedIbanKo = rewardsRepository.findAll(Example.of(RewardsNotification.builder()
                .resultCode(ExportCsvConstants.EXPORT_REJECTION_REASON_IBAN_NOT_FOUND)
                .build())).collectList().block();

        Assertions.assertNotNull(expectedIbanKo);
        Assertions.assertEquals(3, expectedIbanKo.size());
        expectedIbanKo.forEach(r -> {
            Assertions.assertNull(r.getIban());
            Assertions.assertNull(r.getCheckIbanResult());
            Assertions.assertEquals(rule3.getInitiativeId(), r.getInitiativeId());
            Assertions.assertEquals(RewardNotificationStatus.ERROR, r.getStatus());
            Assertions.assertEquals(ExportCsvConstants.EXPORT_REJECTION_REASON_IBAN_NOT_FOUND, r.getRejectionReason());
            Assertions.assertEquals(ExportCsvConstants.EXPORT_REJECTION_REASON_IBAN_NOT_FOUND, r.getResultCode());
            Assertions.assertEquals(YESTERDAY, r.getNotificationDate());
            Assertions.assertEquals(TODAY, r.getExportDate().toLocalDate());
        });

        // check not export
        Assertions.assertEquals(0,
                exportsRepository.findAll(Example.of(RewardOrganizationExport.builder().initiativeId(rule3.getInitiativeId()).build())).count().block()
        );
    }

    private void checkCfKoUseCases(List<RewardOrganizationExport> result) {
        List<RewardsNotification> expectedCfKo = rewardsRepository.findAll(Example.of(RewardsNotification.builder()
                .resultCode(ExportCsvConstants.EXPORT_REJECTION_REASON_CF_NOT_FOUND)
                .build())).collectList().block();

        Assertions.assertNotNull(expectedCfKo);
        Assertions.assertEquals(1, expectedCfKo.size());
        expectedCfKo.forEach(r -> {
            Assertions.assertEquals("IBAN_%s".formatted(r.getBeneficiaryId()), r.getIban());
            Assertions.assertEquals("CHECKIBAN_OUTCOME_%s".formatted(r.getBeneficiaryId()), r.getCheckIbanResult());
            Assertions.assertEquals(rule4.getInitiativeId(), r.getInitiativeId());
            Assertions.assertEquals(RewardNotificationStatus.ERROR, r.getStatus());
            Assertions.assertEquals(ExportCsvConstants.EXPORT_REJECTION_REASON_CF_NOT_FOUND, r.getRejectionReason());
            Assertions.assertEquals(ExportCsvConstants.EXPORT_REJECTION_REASON_CF_NOT_FOUND, r.getResultCode());
            Assertions.assertEquals(YESTERDAY, r.getNotificationDate());
            Assertions.assertEquals(TODAY, r.getExportDate().toLocalDate());
        });

        checkExport(result,
                "INITIATIVEID4_%s.1".formatted(TODAY_STR)
                , rule4, 1
                , "rewards/notifications/ORGANIZATION_ID_4_fwi/INITIATIVEID4/export/NAME_4_wfp_%s.1.zip".formatted(TODAY_STR)
                , TODAY, splitSize);
    }

    private void checkSuspendedUseCases() {
        List<RewardsNotification> expectedSuspended = rewardsRepository.findByInitiativeIdAndNotificationDate("INITIATIVEID5", YESTERDAY).collectList().block();

        Assertions.assertNotNull(expectedSuspended);
        Assertions.assertEquals(1, expectedSuspended.size());
        expectedSuspended.forEach(n ->
                Assertions.assertEquals(RewardNotificationStatus.SUSPENDED, n.getStatus())
        );
    }

    private void checkNoRewardedUseCases() {
        List<RewardsNotification> expectedSkipped = rewardsRepository.findAll(Example.of(RewardsNotification.builder()
                .status(RewardNotificationStatus.SKIPPED)
                .build())).collectList().block();

        Set<String> skippedRewardsInitiativeIds = Set.of(rule0.getInitiativeId(), rule00.getInitiativeId(), rule1.getInitiativeId());

        Assertions.assertNotNull(expectedSkipped);
        Assertions.assertEquals(5, expectedSkipped.size());
        expectedSkipped.forEach(r -> {
            Assertions.assertNull(r.getIban());
            Assertions.assertNull(r.getCheckIbanResult());
            Assertions.assertTrue(skippedRewardsInitiativeIds.contains(r.getInitiativeId()), "Unexpected initiativeId skipped: " + r.getInitiativeId());
            Assertions.assertEquals(RewardNotificationStatus.SKIPPED, r.getStatus());
            Assertions.assertNull(r.getRejectionReason());
            Assertions.assertNull(r.getResultCode());
            Assertions.assertEquals(YESTERDAY, r.getNotificationDate());
            Assertions.assertEquals(TODAY, r.getExportDate().toLocalDate());
        });

        // check not export
        Assertions.assertEquals(0,
                exportsRepository.findAll(Example.of(RewardOrganizationExport.builder().initiativeId(rule3.getInitiativeId()).build())).count().block()
        );

        Assertions.assertNull(exportsRepository.findById("STUCKEXPORTIDWithoutRewards.1").block());
    }

    @SneakyThrows
    private void checkKoNotification() {
        int ibanKo = 0;
        int cfKo = 0;

        for (ConsumerRecord<String, String> msg : consumeMessages(topicRewardNotificationFeedback, 4, 1000)) {
            RewardFeedbackDTO n = objectMapper.readValue(msg.value(), RewardFeedbackDTO.class);

            Assertions.assertEquals(RewardFeedbackMapper.REWARD_NOTIFICATION_FEEDBACK_STATUS_REJECTED, n.getStatus());

            if (ExportCsvConstants.EXPORT_REJECTION_REASON_IBAN_NOT_FOUND.equals(n.getRejectionCode())) {
                Assertions.assertEquals("INITIATIVEID3", n.getInitiativeId());

                ibanKo++;
            } else if (ExportCsvConstants.EXPORT_REJECTION_REASON_CF_NOT_FOUND.equals(n.getRejectionCode())) {
                Assertions.assertEquals("INITIATIVEID4", n.getInitiativeId());

                cfKo++;
            } else {
                Assertions.fail("Unexpected rejection code: %s".formatted(n));
            }

            Assertions.assertTrue(n.getOrganizationId().startsWith("ORGANIZATION_ID_"), "Unexpected organizationId: %s".formatted(n));

            Assertions.assertNotNull(n.getExternalId());
            Assertions.assertNotNull(n.getRewardNotificationId());
            Assertions.assertNotNull(n.getUserId());
            Assertions.assertNotEquals(0, n.getEffectiveRewardCents());

            Assertions.assertNull(n.getCro());
            Assertions.assertNull(n.getExecutionDate());

            Assertions.assertEquals(LocalDate.now(), n.getFeedbackDate().toLocalDate());
            Assertions.assertEquals(0, n.getFeedbackProgressive());
            Assertions.assertEquals(0L, n.getRewardCents());
            Assertions.assertEquals(n.getRejectionCode(), n.getRejectionReason());

            Assertions.assertEquals("%s_%s".formatted(n.getUserId(), n.getInitiativeId()), msg.key());
        }

        Assertions.assertEquals(3, ibanKo);
        Assertions.assertEquals(1, cfKo);

    }

    private String buildBeneficiaryIdFromUseCase(boolean hasCf, boolean isDiscount, long bias) {
        String beneficiaryIdPrefix = isDiscount ? "MERCHANTID" : "USERID";
        String hasCfString = hasCf ? "OK" : "NOTFOUND";
        return "%s_%s_%d".formatted(beneficiaryIdPrefix, hasCfString, bias);
    }

    private String replaceId(String notificationId, boolean hasCf, boolean isDiscount) {
        String beneficiaryIdPrefix = isDiscount ? "MERCHANTID" : "USERID";
        String hasCfString = hasCf ? "OK" : "NOTFOUND";
        return notificationId.replace(beneficiaryIdPrefix, "%s_%s_".formatted(beneficiaryIdPrefix, hasCfString));
    }
}
