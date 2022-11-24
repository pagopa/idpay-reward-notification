package it.gov.pagopa.reward.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageEventDto {
    private String id;
    private String subject;
    private String eventType;
    private StorageEventData data;
    private OffsetDateTime eventTime;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StorageEventData {
        private String eTag;
        private Integer contentLength;
        private String url;

    }
}
