package it.gov.pagopa.reward.notification.dto.mapper;

import it.gov.pagopa.reward.notification.dto.controller.RewardExportsDTO;
import it.gov.pagopa.reward.notification.enums.ExportStatus;
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
                .status(ExportStatus.COMPLETE)
                .build();

        // When
        RewardExportsDTO result = mapper.apply(rewardOrganizationExport);

        // Then
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result);
    }
}