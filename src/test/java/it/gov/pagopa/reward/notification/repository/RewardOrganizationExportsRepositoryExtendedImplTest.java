package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.BaseIntegrationTestDeprecated;
import it.gov.pagopa.reward.notification.dto.controller.ExportFilter;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationExportStatus;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

class RewardOrganizationExportsRepositoryExtendedImplTest extends BaseIntegrationTestDeprecated {

    public static final LocalDate TODAY = LocalDate.now();
    public static final String TEST_EXPORT_ID = "TEST_EXPORT";
    public static final String TEST_INITIATIVE_ID = "TEST_INITIATIVE";
    public static final String TEST_ORGANIZATION_ID = "TEST_ORGANIZATION";
    public static final LocalDate TEST_DATE_FROM = LocalDate.of(2001,1,1);


    @Autowired
    private RewardOrganizationExportsRepository rewardOrganizationExportsRepository;
    @Autowired
    private RewardOrganizationExportsRepositoryExtendedImpl rewardOrganizationExportsRepositoryExtended;

    @BeforeEach
    void prepareTestData() {
        rewardOrganizationExportsRepository.save(
                buildUseCase(TEST_EXPORT_ID, TEST_INITIATIVE_ID, TEST_ORGANIZATION_ID, RewardOrganizationExportStatus.EXPORTED)
        ).block();
    }

    @AfterEach
    void cleanData() {
        rewardOrganizationExportsRepository.deleteById(TEST_EXPORT_ID);
    }

    @Test
    void testFindAllBy() {
        String organizationId = TEST_ORGANIZATION_ID;
        String initiativeId = TEST_INITIATIVE_ID;
        Pageable pageable = null;
        ExportFilter filters = null;

        List<RewardOrganizationExport> result = rewardOrganizationExportsRepository
                .findAllBy(organizationId, initiativeId, pageable, filters)
                .collectList()
                .block();

        checkResult(result, TEST_EXPORT_ID);
    }

    @Test
    void testFindAllByWithFilters() {
        String organizationId = TEST_ORGANIZATION_ID;
        String initiativeId = TEST_INITIATIVE_ID;
        Pageable pageable = null;
        // all filters
        ExportFilter filters1 = ExportFilter.builder()
                .status("EXPORTED")
                .notificationDateFrom(TEST_DATE_FROM)
                .notificationDateTo(TODAY)
                .build();
        // not valid status
        ExportFilter filters2 = ExportFilter.builder()
                .status("IN_PROGRESS")
                .notificationDateFrom(TEST_DATE_FROM)
                .notificationDateTo(TODAY)
                .build();
        // no status
        ExportFilter filters3 = ExportFilter.builder()
                .notificationDateFrom(TEST_DATE_FROM)
                .notificationDateTo(TODAY)
                .build();


        List<RewardOrganizationExport> result1 = rewardOrganizationExportsRepository
                .findAllBy(organizationId, initiativeId, pageable, filters1)
                .collectList()
                .block();
        List<RewardOrganizationExport> result2 = rewardOrganizationExportsRepository
                .findAllBy(organizationId, initiativeId, pageable, filters2)
                .collectList()
                .block();
        List<RewardOrganizationExport> result3 = rewardOrganizationExportsRepository
                .findAllBy(organizationId, initiativeId, pageable, filters3)
                .collectList()
                .block();

        // result 1 OK
        checkResult(result1, TEST_EXPORT_ID);
        // result 2 empty list
        Assertions.assertNotNull(result2);
        Assertions.assertTrue(result2.isEmpty());
        // result 3 OK
        checkResult(result3, TEST_EXPORT_ID);
    }

    @Test
    void testCountAll() {
        String organizationId = TEST_ORGANIZATION_ID;
        String initiativeId = TEST_INITIATIVE_ID;
        ExportFilter filters = null;

        Long result = rewardOrganizationExportsRepository
                .countAll(organizationId, initiativeId, filters)
                .block();

        Assertions.assertEquals(1, result);
    }

    private RewardOrganizationExport buildUseCase(String id, String initiativeId, String organizationId, RewardOrganizationExportStatus status) {
        return RewardOrganizationExport.builder()
                .id(id)
                .initiativeId(initiativeId)
                .initiativeName("%s_NAME".formatted(initiativeId))
                .organizationId(organizationId)
                .filePath("/%s/%s".formatted(organizationId, id))
                .notificationDate(LocalDate.of(2001,2,4))
                .rewardsExportedCents(100L)
                .rewardsResultsCents(100L)
                .rewardNotified(1L)
                .rewardsResulted(1L)
                .rewardsResultedOk(1L)
                .percentageResulted(10000L)
                .percentageResultedOk(10000L)
                .percentageResults(10000L)
                .feedbackDate(LocalDateTime.now())
                .status(status)
                .build();
    }

    private static void checkResult(List<RewardOrganizationExport> result, String expectedId) {
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(expectedId, result.get(0).getId());
    }
}
