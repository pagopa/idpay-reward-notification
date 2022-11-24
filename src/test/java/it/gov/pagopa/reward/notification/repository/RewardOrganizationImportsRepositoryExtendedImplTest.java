package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.dto.controller.FeedbackImportFilter;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationImportStatus;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

class RewardOrganizationImportsRepositoryExtendedImplTest extends BaseIntegrationTest {

    public static final LocalDateTime TODAY = LocalDateTime.now();
    public static final String TEST_IMPORT_FILE_PATH = "test/import";
    public static final String TEST_INITIATIVE_ID = "TEST_INITIATIVE";
    public static final String TEST_ORGANIZATION_ID = "TEST_ORGANIZATION";
    public static final LocalDateTime TEST_DATE_FROM = TODAY.minusDays(5);

    private static final RewardOrganizationImport testImport1 = RewardOrganizationImport.builder()
            .filePath("%s_1".formatted(TEST_IMPORT_FILE_PATH))
            .initiativeId(TEST_INITIATIVE_ID)
            .organizationId(TEST_ORGANIZATION_ID)
            .feedbackDate(TODAY)
            .eTag("ETAG")
            .contentLength(5)
            .url("URL")
            .elabDate(TODAY)
            .status(RewardOrganizationImportStatus.COMPLETE)
            .errors(List.of(new RewardOrganizationImport.RewardOrganizationImportError()))
            .build();

    private static final RewardOrganizationImport testImport2 = RewardOrganizationImport.builder()
            .filePath("%s_2".formatted(TEST_IMPORT_FILE_PATH))
            .initiativeId(TEST_INITIATIVE_ID)
            .organizationId(TEST_ORGANIZATION_ID)
            .feedbackDate(TODAY)
            .eTag("ETAG")
            .contentLength(5)
            .url("URL")
            .elabDate(LocalDateTime.of(2015,5,31,0,0))
            .status(RewardOrganizationImportStatus.COMPLETE)
            .errors(List.of(new RewardOrganizationImport.RewardOrganizationImportError()))
            .build();

    private static final RewardOrganizationImport testImport3 = RewardOrganizationImport.builder()
            .filePath("%s_3".formatted(TEST_IMPORT_FILE_PATH))
            .initiativeId(TEST_INITIATIVE_ID)
            .organizationId(TEST_ORGANIZATION_ID)
            .feedbackDate(TODAY)
            .eTag("ETAG")
            .contentLength(5)
            .url("URL")
            .elabDate(TODAY)
            .status(RewardOrganizationImportStatus.IN_PROGRESS)
            .errors(List.of(new RewardOrganizationImport.RewardOrganizationImportError()))
            .build();

    List<RewardOrganizationImport> testData = List.of(testImport1,testImport2,testImport3);

    @Autowired
    private RewardOrganizationImportsRepository importsRepository;
    @Autowired
    private RewardOrganizationImportsRepositoryExtendedImpl extendedRepository;

    @BeforeEach
    void prepareTestData() {
        importsRepository.saveAll(testData).collectList().block();
    }

    @AfterEach
    void cleanData() { importsRepository.deleteAll(testData); }

    @Test
    void testFindAllBy() {

        List<RewardOrganizationImport> result = importsRepository
                .findAllBy(TEST_ORGANIZATION_ID, TEST_INITIATIVE_ID, null, null)
                .collectList()
                .block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.size());
        checkFindAllResult(result);
    }

    @Test
    void testFindAllByWithFilters() {
        // all filters
        FeedbackImportFilter filters = FeedbackImportFilter.builder()
                .status("COMPLETE")
                .elabDateFrom(TEST_DATE_FROM)
                .elabDateTo(TODAY.plusDays(5))
                .build();


        List<RewardOrganizationImport> result = importsRepository
                .findAllBy(TEST_ORGANIZATION_ID, TEST_INITIATIVE_ID, null, filters)
                .collectList()
                .block();

        // result 1 OK
        Assertions.assertNotNull(result);
        checkFindWithFiltersResult(result);
    }

    @Test
    void testCountAll() {

        Long result = importsRepository
                .countAll(TEST_ORGANIZATION_ID, TEST_INITIATIVE_ID, null)
                .block();

        Assertions.assertEquals(3, result);
    }

    @Test
    void testFindByImportId() {
        String importId = "%s_1".formatted(TEST_IMPORT_FILE_PATH);

        RewardOrganizationImport result = importsRepository
                .findByImportId(TEST_ORGANIZATION_ID, TEST_INITIATIVE_ID, importId)
                .block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(testImport1.getFilePath(), result.getFilePath());
        Assertions.assertEquals(testImport1.getInitiativeId(), result.getInitiativeId());
        Assertions.assertEquals(testImport1.getOrganizationId(), result.getOrganizationId());

    }

    private void checkFindAllResult(List<RewardOrganizationImport> result) {
        for(RewardOrganizationImport r : result) {
            Assertions.assertNotNull(r);

            String[] filePathSplit = r.getFilePath().split("_");
            String filePathRoot = filePathSplit[0];
            Assertions.assertEquals(TEST_IMPORT_FILE_PATH, filePathRoot);
        }
    }

    private static void checkFindWithFiltersResult(List<RewardOrganizationImport> resultList) {
        Assertions.assertEquals(1, resultList.size());

        RewardOrganizationImport result = resultList.get(0);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(testImport1.getFilePath(), result.getFilePath());
        Assertions.assertEquals(testImport1.getInitiativeId(), result.getInitiativeId());
        Assertions.assertEquals(testImport1.getOrganizationId(), result.getOrganizationId());
        Assertions.assertNull(result.getErrors());
    }
}