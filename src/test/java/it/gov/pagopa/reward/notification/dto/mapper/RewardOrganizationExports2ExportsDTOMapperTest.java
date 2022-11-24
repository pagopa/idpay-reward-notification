package it.gov.pagopa.reward.notification.dto.mapper;

import it.gov.pagopa.reward.notification.dto.controller.RewardExportsDTO;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationExportStatus;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.test.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

class RewardOrganizationExports2ExportsDTOMapperTest {

    private final RewardOrganizationExports2ExportsDTOMapper mapper = new RewardOrganizationExports2ExportsDTOMapper();

    @Test
    void test() {
        // Given
        RewardOrganizationExport rewardOrganizationExport = RewardOrganizationExport.builder()
                .id("ID")
                .initiativeId("INITIATIVE_ID")
                .initiativeName("INITIATIVE_NAME")
                .organizationId("ORGANIZATION_ID")
                .filePath("/test/file")
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
                .status(RewardOrganizationExportStatus.COMPLETE)
                .build();

        // When
        RewardExportsDTO result = mapper.apply(rewardOrganizationExport);

        // Then
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result);
        checkFields(result, rewardOrganizationExport);
    }

    private void checkFields(RewardExportsDTO result, RewardOrganizationExport input) {
        Assertions.assertEquals(input.getId(), result.getId());
        Assertions.assertEquals(input.getInitiativeId(), result.getInitiativeId());
        Assertions.assertEquals(input.getInitiativeName(), result.getInitiativeName());
        Assertions.assertEquals(input.getOrganizationId(), result.getOrganizationId());
        Assertions.assertEquals("file", result.getFilePath());
        Assertions.assertEquals(input.getNotificationDate(), result.getNotificationDate());
        Assertions.assertEquals("1,00", result.getRewardsExported());
        Assertions.assertEquals("1,00", result.getRewardsResults());
        Assertions.assertEquals(input.getRewardNotified(), result.getRewardNotified());
        Assertions.assertEquals(input.getRewardsResulted(), result.getRewardsResulted());
        Assertions.assertEquals(input.getRewardsResultedOk(), result.getRewardsResultedOk());
        Assertions.assertEquals("100", result.getPercentageResulted());
        Assertions.assertEquals("100", result.getPercentageResultedOk());
        Assertions.assertEquals("100", result.getPercentageResults());
        Assertions.assertEquals(input.getFeedbackDate(), result.getFeedbackDate());
        Assertions.assertEquals(input.getStatus(), result.getStatus());
    }
}