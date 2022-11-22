package it.gov.pagopa.reward.notification.test.fakers;

import it.gov.pagopa.reward.notification.dto.StorageEventDto;
import it.gov.pagopa.reward.notification.utils.RewardFeedbackConstants;

import java.time.OffsetDateTime;

public class StorageEventDtoFaker {
    public static StorageEventDto mockInstance(Integer bias) {
        return StorageEventDto.builder()
                .id("ID%d".formatted(bias))
                .eventType(RewardFeedbackConstants.AZURE_STORAGE_EVENT_TYPE_BLOB_CREATED)
                .subject(RewardFeedbackConstants.AZURE_STORAGE_SUBJECT_PREFIX + "orgId/initiativeId/import/reward-dispositive-%d.zip".formatted(bias))
                .data(StorageEventDto.StorageEventData.builder()
                        .eTag("ETAG%d".formatted(bias))
                        .contentLength(1000)
                        .url("https://STORAGEACCOUNT.blob.core.windows.net/CONTAINERNAME/orgId/initiativeId/import/reward-dispositive-%d.zip".formatted(bias))
                        .build())
                .eventTime(OffsetDateTime.now().plusSeconds(bias))
                .build();
    }
}
