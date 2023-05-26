package it.gov.pagopa.reward.notification.dto.mapper;

import it.gov.pagopa.reward.notification.dto.controller.RewardImportsDTO;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationImportStatus;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.common.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

class RewardOrganizationImport2ImportsDTOMapperTest {

    private final RewardOrganizationImport2ImportsDTOMapper mapper = new RewardOrganizationImport2ImportsDTOMapper();

    @Test
    void test() {
        // Given
        LocalDateTime CHOSEN_DATE = LocalDateTime.of(2022,7,18,12,0);

        RewardOrganizationImport input = RewardOrganizationImport.builder()
                .filePath("file/path")
                .initiativeId("INITIATIVEID")
                .organizationId("ORGANIZATIONID")
                .feedbackDate(CHOSEN_DATE)
                .eTag("ETAG")
                .contentLength(1)
                .url("URL")
                .rewardsResulted(10L)
                .rewardsResultedOk(2L)
                .rewardsResultedError(0L)
                .rewardsResultedOkError(0L)
                .percentageResulted(10000L)
                .percentageResultedOk(2000L)
                .percentageResultedOkElab(2000L)
                .elabDate(CHOSEN_DATE)
                .exportIds(Collections.emptyList())
                .status(RewardOrganizationImportStatus.COMPLETE)
                .errorsSize(1)
                .errors(List.of(new RewardOrganizationImport.RewardOrganizationImportError()))
                .build();

        // When
        RewardImportsDTO result = mapper.apply(input);

        // Then
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result);
        checkResult(result, input);
    }

    private void checkResult(RewardImportsDTO result, RewardOrganizationImport input) {
        Assertions.assertEquals("path", result.getFilePath());
        Assertions.assertEquals(input.getInitiativeId(), result.getInitiativeId());
        Assertions.assertEquals(input.getOrganizationId(), result.getOrganizationId());
        Assertions.assertEquals(input.getFeedbackDate(), result.getFeedbackDate());
        Assertions.assertEquals(input.getETag(), result.getETag());
        Assertions.assertEquals(input.getContentLength(), result.getContentLength());
        Assertions.assertEquals(input.getRewardsResulted(), result.getRewardsResulted());
        Assertions.assertEquals(input.getRewardsResultedOk(), result.getRewardsResultedOk());
        Assertions.assertEquals(input.getRewardsResultedError(), result.getRewardsResultedOkError());
        Assertions.assertEquals(input.getRewardsResultedOkError(), result.getRewardsResultedOkError());
        Assertions.assertEquals("100", result.getPercentageResulted());
        Assertions.assertEquals("20", result.getPercentageResultedOk());
        Assertions.assertEquals("20", result.getPercentageResultedOkElab());
        Assertions.assertEquals(input.getElabDate(), result.getElabDate());
        Assertions.assertEquals(input.getExportIds(), result.getExportIds());
        Assertions.assertEquals(input.getStatus(), result.getStatus());
        Assertions.assertEquals(input.getErrorsSize(), result.getErrorsSize());
    }
}