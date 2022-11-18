package it.gov.pagopa.reward.notification.dto.controller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportFilter {

    private String status;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDateTime elabDateFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDateTime elabDateTo;
}
