package it.gov.pagopa.reward.notification.dto.mapper;

import it.gov.pagopa.reward.notification.dto.StorageEventDto;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationImportStatus;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.utils.RewardFeedbackConstants;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class StorageEvent2OrganizationImportMapper implements Function<StorageEventDto, RewardOrganizationImport> {

    @Override
    public RewardOrganizationImport apply(StorageEventDto storageEventDto) {
        String[] pathSplits = storageEventDto.getSubject().split("/");

        if(pathSplits.length != 10 || !pathSplits[8].equals("import")){
            throw new IllegalArgumentException("Unexpected file location: %s".formatted(storageEventDto.getSubject()));
        }

        return RewardOrganizationImport.builder()
                .id(storageEventDto.getId())
                .initiativeId(pathSplits[7])
                .organizationId(pathSplits[6])
                .feedbackDate(storageEventDto.getEventTime().toLocalDateTime())
                .filePath(storageEventDto.getSubject().replace(RewardFeedbackConstants.AZURE_STORAGE_SUBJECT_PREFIX, ""))
                .eTag(storageEventDto.getData().getETag())
                .contentLength(storageEventDto.getData().getContentLength())
                .url(storageEventDto.getData().getUrl())
                .status(RewardOrganizationImportStatus.IN_PROGRESS)
                .build();
    }
}
