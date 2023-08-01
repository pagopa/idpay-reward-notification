package it.gov.pagopa.reward.notification.dto.commands;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
public class CommandOperationDTO {
    private String operationId;
    private String operationType;
    private LocalDateTime operationTime;
}
