package it.gov.pagopa.reward.notification.dto.controller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportFilter {

    private String iban;
    private String status;
    private LocalDateTime notificationDateFrom;
    private LocalDateTime notificationDateTo;
}
