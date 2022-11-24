package it.gov.pagopa.reward.notification.dto.mapper;

import it.gov.pagopa.reward.notification.dto.controller.FeedbackImportErrorsCsvDTO;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RewardOrganizationImportErrors2ErrorsCsvMapper {

    public List<FeedbackImportErrorsCsvDTO> apply(List<RewardOrganizationImport.RewardOrganizationImportError> errors) {
        List<FeedbackImportErrorsCsvDTO> out = new ArrayList<>();

        for (RewardOrganizationImport.RewardOrganizationImportError e : errors) {
            out.add(
                    new FeedbackImportErrorsCsvDTO(
                            e.getRow(),
                            e.getErrorCode(),
                            e.getErrorDescription()
                    )
            );
        }

        return out;
    }
}
