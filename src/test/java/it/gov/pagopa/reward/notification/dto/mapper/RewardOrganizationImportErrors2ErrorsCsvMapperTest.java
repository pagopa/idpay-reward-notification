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
        List<RewardOrganizationImport.RewardOrganizationImportError> errors = List.of(
                RewardOrganizationImport.RewardOrganizationImportError.builder()
                        .row(1)
                        .errorCode("CODE")
                        .errorDescription("ERROR")
                        .build()
        );

        // When
        List<FeedbackImportErrorsCsvDTO> resultList = mapper.apply(errors);

        // Then
        Assertions.assertNotNull(resultList);
        Assertions.assertEquals(1, resultList.size());

        RewardOrganizationImport.RewardOrganizationImportError expected = errors.get(0);
        FeedbackImportErrorsCsvDTO result = resultList.get(0);
        Assertions.assertEquals(expected.getRow(), result.getRow());
        Assertions.assertEquals(expected.getErrorCode(), result.getErrorCode());
        Assertions.assertEquals(expected.getErrorDescription(), result.getErrorDescription());
    }

}