package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.dto.controller.ImportFilter;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationImportStatus;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.List;

class RewardOrganizationImportsRepositoryExtendedImplTest extends BaseIntegrationTest {

    public static final LocalDateTime TODAY = LocalDateTime.now();
    public static final String TEST_IMPORT_FILE_PATH = "test/import";
    public static final String TEST_INITIATIVE_ID = "TEST_INITIATIVE";
    public static final String TEST_ORGANIZATION_ID = "TEST_ORGANIZATION";

    private static final RewardOrganizationImport testImport = RewardOrganizationImport.builder()
            .filePath(TEST_IMPORT_FILE_PATH)
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

    @Autowired
    private RewardOrganizationImportsRepository importsRepository;
    @Autowired
    private RewardOrganizationImportsRepositoryExtendedImpl extendedRepository;

    @BeforeEach
    void prepareTestData() {
        importsRepository.save(testImport).block();
    }

    @AfterEach
    void cleanData() { importsRepository.deleteById(TEST_IMPORT_FILE_PATH); }

    @Test
    void testFindAllBy() {

        List<RewardOrganizationImport> result = extendedRepository
                .findAllBy(TEST_ORGANIZATION_ID, TEST_INITIATIVE_ID, null, null)
                .collectList()
                .block();

        Assertions.assertNotNull(result);
        checkResult(result);
    }

    @Test
    void testCountAll() {

        Long result = extendedRepository
                .countAll(TEST_ORGANIZATION_ID, TEST_INITIATIVE_ID, null)
                .block();

        Assertions.assertEquals(1, result);
    }

    private static void checkResult(List<RewardOrganizationImport> resultList) {
        Assertions.assertEquals(1, resultList.size());

        RewardOrganizationImport result = resultList.get(0);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(testImport.getFilePath(), result.getFilePath());
        Assertions.assertEquals(testImport.getInitiativeId(), result.getInitiativeId());
        Assertions.assertEquals(testImport.getOrganizationId(), result.getOrganizationId());
        Assertions.assertNull(result.getErrors());
    }
}