package it.gov.pagopa.reward.notification.dto.commands;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@Builder
public class CommandOperationDTO {
    private String entityId;
    private String operationType;
    private LocalDateTime operationTime;
    private Map<String, String> additionalParams;
}
