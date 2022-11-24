package it.gov.pagopa.reward.notification.dto.mapper;

import it.gov.pagopa.reward.notification.dto.controller.FeedbackImportErrorsCsvDTO;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import org.springframework.stereotype.Service;

@Service
public class RewardOrganizationImportErrors2ErrorsCsvMapper {

    public FeedbackImportErrorsCsvDTO apply(RewardOrganizationImport.RewardOrganizationImportError error) {

        return new FeedbackImportErrorsCsvDTO(
                error.getRow(),
                error.getErrorCode(),
                error.getErrorDescription());

    }
}
