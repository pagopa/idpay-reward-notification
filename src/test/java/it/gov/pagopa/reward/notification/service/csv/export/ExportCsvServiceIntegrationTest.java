package it.gov.pagopa.reward.notification.service.csv.export;

import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.enums.ExportStatus;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardNotificationRuleRepository;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationExportsRepository;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.test.fakers.RewardNotificationRuleFaker;
import it.gov.pagopa.reward.notification.test.fakers.RewardsNotificationFaker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Example;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

@TestPropertySource(properties = {
        "app.csv.export.split-size=5000"
})
class ExportCsvServiceIntegrationTest extends BaseIntegrationTest {

    public static final LocalDate TODAY = LocalDate.now();

    @Value("${app.csv.export.day-before}")
    private int dayBefore;
    @Value("${app.csv.export.split-size}")
    private int splitSize;

    private long N;

    private LocalDate stuckNotificationDate;

    @Autowired
    private RewardNotificationRuleRepository ruleRepository;
    @Autowired
    private RewardsNotificationRepository rewardsRepository;
    @Autowired
    private RewardOrganizationExportsRepository exportsRepository;

    @Autowired
    private ExportCsvService exportCsvService;

    private void storeTestData() {
        N = (long) splitSize * 2 + 7;
        stuckNotificationDate = TODAY.minusDays(dayBefore);

        ruleRepository.save(RewardNotificationRuleFaker.mockInstanceBuilder(0).initiativeId("INITIATIVEID").build()).block();
        ruleRepository.save(RewardNotificationRuleFaker.mockInstanceBuilder(0).initiativeId("INITIATIVEID2").build()).block();

        // useCase rewards not to be notified
        rewardsRepository.saveAll(IntStream.range(0, (int) N).mapToObj(RewardsNotificationFaker::mockInstance).toList()).collectList().block();

        //TODO store Iban e mock PSV

        //TODO useCase iban and PDV Errors

        // useCase rewards to be notified in the future
        rewardsRepository.saveAll(LongStream.range(N, 2*N).mapToObj(bias -> RewardsNotificationFaker.mockInstanceBuilder((int) bias)
                .rewardCents(bias)
                .notificationDate(TODAY.plusDays(1))
                .build()
        ).toList()).collectList().block();

        // useCase new rewards to notify
        rewardsRepository.saveAll(LongStream.range(2*N, 3*N).mapToObj(bias -> RewardsNotificationFaker.mockInstanceBuilder((int) bias)
                .rewardCents(bias)
                .notificationDate(TODAY)
                .build()
        ).toList()).collectList().block();

        // useCase rewards partially related to a stuck export
        rewardsRepository.saveAll(LongStream.range(3*N, 4*N).mapToObj(bias -> RewardsNotificationFaker.mockInstanceBuilder((int) bias)
                .initiativeId("INITIATIVEID2")
                .rewardCents(bias)
                .exportId(bias < N / 2 ? "STUCKEXPORTID" : null)
                .notificationDate(stuckNotificationDate)
                .build()
        ).toList()).collectList().block();

        // stuck export
        exportsRepository.save(RewardOrganizationExport.builder()
                .id("STUCKEXPORTID.5")
                .progressive(5)
                .filePath("5.zip")
                .initiativeId("INITIATIVEID2")
                .notificationDate(stuckNotificationDate)
                .status(ExportStatus.IN_PROGRESS)
                .build()).block();
    }

    @AfterEach
    void clearData() {
        ruleRepository.deleteAll().block();
        rewardsRepository.deleteAll().block();
        exportsRepository.deleteAll().block();
    }

    @Test
    void test() {
        // Given
        storeTestData();

        // When
        List<RewardOrganizationExport> result = exportCsvService.execute().collectList().block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3 * 2, result.size());

        List<RewardOrganizationExport> exports = exportsRepository.findAll().collectList().block();
        Assertions.assertEquals(exports, result);

        checkInitiativeExports("INITIATIVEID", "INITIATIVENAME", "ORGANIZATIONID", "0.zip", "EXPORTID.0", 0L, TODAY, result);
        checkInitiativeExports("INITIATIVEID2", null, null, "5.zip", "STUCKEXPORTID.5", 5L, stuckNotificationDate, result);
    }

    private void checkInitiativeExports(String initiativeId, String initiativeName, String organizationId, String expectedBaseFilePath, String expectedBaseExportId, long expectedBaseProgressive, LocalDate expectedNotificationDate, List<RewardOrganizationExport> result) {
        // base export
        checkExport(result,
                expectedBaseExportId,
                initiativeId, initiativeName, organizationId,
                expectedBaseProgressive, expectedBaseFilePath, expectedNotificationDate, splitSize);
        // split 1
        checkExportSplit(result, 1, splitSize,
                expectedBaseExportId,
                initiativeId, initiativeName, organizationId,
                expectedBaseProgressive, expectedBaseFilePath, expectedNotificationDate);
        // split 2
        checkExportSplit(result, 2, (int) (N%splitSize),
                expectedBaseExportId,
                initiativeId, initiativeName, organizationId,
                expectedBaseProgressive, expectedBaseFilePath, expectedNotificationDate);

        Assertions.assertEquals(N * (N + 1) / 2,
                result.stream().filter(e -> initiativeId.equals(e.getInitiativeId())).mapToLong(RewardOrganizationExport::getRewardsExportedCents).sum()
        );
    }

    private void checkExport(List<RewardOrganizationExport> result, String exportId, String initiativeId, String initiativeName, String organizationId, long expectedProgressive, String expectedFilePath, LocalDate expectedNotificationDate, long expectedExportSize) {
        RewardOrganizationExport export = result.stream().filter(e -> exportId.equals(e.getId())).findFirst().orElse(null);
        Assertions.assertNotNull(export);

        Assertions.assertEquals(initiativeId, export.getInitiativeId());
        Assertions.assertEquals(initiativeName, export.getInitiativeName());
        Assertions.assertEquals(organizationId, export.getOrganizationId());
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
        Assertions.assertEquals(ExportStatus.EXPORTED, export.getStatus());

        List<RewardsNotification> rewards = rewardsRepository.findAll(Example.of(RewardsNotification.builder().exportId(exportId).build())).collectList().block();
        Assertions.assertNotNull(rewards);
        Assertions.assertEquals(expectedExportSize, rewards.size());
        rewards.forEach(r-> {
            Assertions.assertEquals(RewardNotificationStatus.EXPORTED, r.getStatus());
            Assertions.assertEquals(LocalDate.now(), r.getExportDate().toLocalDate());
        });

        // TODO check CSV
    }

    private void checkExportSplit(List<RewardOrganizationExport> result, int splitNumber, int splitSize, String expectedBaseExportId, String initiativeId, String initiativeName, String organizationId, long expectedBaseProgressive, String expectedBaseFilePath, LocalDate expectedNotificationDate){
        checkExport(result,
                expectedBaseExportId.replaceFirst("\\.%d$".formatted(expectedBaseProgressive), ".%d".formatted(expectedBaseProgressive+ splitNumber)),
                initiativeId, initiativeName, organizationId,
                expectedBaseProgressive+ splitNumber,
                expectedBaseFilePath.replaceFirst("\\.%d.zip$".formatted(expectedBaseProgressive), ".%d.zip".formatted(expectedBaseProgressive+ splitNumber)),
                expectedNotificationDate, splitSize);
    }
}
