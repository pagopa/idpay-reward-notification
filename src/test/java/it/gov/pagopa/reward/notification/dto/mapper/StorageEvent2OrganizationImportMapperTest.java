package it.gov.pagopa.reward.notification.dto.mapper;

import it.gov.pagopa.reward.notification.dto.StorageEventDto;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationImportStatus;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.test.fakers.StorageEventDtoFaker;
import it.gov.pagopa.common.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class StorageEvent2OrganizationImportMapperTest {

    private final StorageEvent2OrganizationImportMapper mapper = new StorageEvent2OrganizationImportMapper();

    @Test
    void testUnexpectedFilePath() {
        // Given
        StorageEventDto dto = StorageEventDtoFaker.mockInstance(0);
        dto.setSubject("UNEXPECTED/PATH");
        try {
            // When
            mapper.apply(dto);

            // Then
            Assertions.fail("Expecting exception to be thrown");
        } catch (IllegalArgumentException e) {
            Assertions.assertEquals("Unexpected file location: UNEXPECTED/PATH", e.getMessage());
        }

        dto.setSubject("/blobServices/default/containers/refund/blobs/orgId/initiativeId/export/reward-dispositive-0.zip");
        try {
            // When
            mapper.apply(dto);

            // Then
            Assertions.fail("Expecting exception to be thrown");
        } catch (IllegalArgumentException e) {
            Assertions.assertEquals("Unexpected file location: /blobServices/default/containers/refund/blobs/orgId/initiativeId/export/reward-dispositive-0.zip", e.getMessage());
        }
    }

    @Test
    void test() {
        // Given
        StorageEventDto dto = StorageEventDtoFaker.mockInstance(0);

        // When
        RewardOrganizationImport result = mapper.apply(dto);

        // Then
        Assertions.assertNotNull(result);
        checkFields(result, dto);
        TestUtils.checkNotNullFields(result, "elabDate");
    }

    private void checkFields(RewardOrganizationImport result, StorageEventDto dto) {
        Assertions.assertEquals("initiativeId", result.getInitiativeId());
        Assertions.assertEquals("orgId", result.getOrganizationId());
        Assertions.assertEquals(dto.getEventTime().toLocalDateTime(), result.getFeedbackDate());
        Assertions.assertEquals("orgId/initiativeId/import/reward-dispositive-0.zip", result.getFilePath());
        Assertions.assertEquals(dto.getData().getETag(), result.getETag());
        Assertions.assertEquals(dto.getData().getContentLength(), result.getContentLength());
        Assertions.assertEquals(dto.getData().getUrl(), result.getUrl());
        Assertions.assertEquals(0L, result.getRewardsResulted());
        Assertions.assertEquals(0L, result.getRewardsResultedError());
        Assertions.assertEquals(0L, result.getRewardsResultedOk());
        Assertions.assertEquals(0L, result.getRewardsResultedOkError());
        Assertions.assertEquals(0L, result.getPercentageResulted());
        Assertions.assertEquals(0L, result.getPercentageResultedOk());
        Assertions.assertEquals(0L, result.getPercentageResultedOkElab());
        Assertions.assertNull(result.getElabDate());
        Assertions.assertEquals(Collections.emptyList(), result.getExportIds());
        Assertions.assertEquals(RewardOrganizationImportStatus.IN_PROGRESS, result.getStatus());
        Assertions.assertEquals(Collections.emptyList(), result.getErrors());
    }
}
