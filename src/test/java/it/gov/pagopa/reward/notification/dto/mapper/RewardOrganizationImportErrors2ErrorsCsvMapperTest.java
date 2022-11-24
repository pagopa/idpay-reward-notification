package it.gov.pagopa.reward.notification.dto.mapper;

import it.gov.pagopa.reward.notification.dto.controller.FeedbackImportErrorsCsvDTO;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class RewardOrganizationImportErrors2ErrorsCsvMapperTest {

    private final RewardOrganizationImportErrors2ErrorsCsvMapper mapper = new RewardOrganizationImportErrors2ErrorsCsvMapper();

    @Test
    void test() {
        // Given
        RewardOrganizationImport.RewardOrganizationImportError error =
                RewardOrganizationImport.RewardOrganizationImportError.builder()
                        .row(1)
                        .errorCode("CODE")
                        .errorDescription("ERROR")
                        .build();

        // When
        FeedbackImportErrorsCsvDTO result = mapper.apply(error);

        // Then
        Assertions.assertNotNull(result);

        Assertions.assertEquals(error.getRow(), result.getRow());
        Assertions.assertEquals(error.getErrorCode(), result.getErrorCode());
        Assertions.assertEquals(error.getErrorDescription(), result.getErrorDescription());
    }

}