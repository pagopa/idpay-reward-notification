package it.gov.pagopa.reward.notification.dto.controller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportFilter {

    private String status;
    private LocalDate notificationDateFrom;
    private LocalDate notificationDateTo;
}
