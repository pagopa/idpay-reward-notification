package it.gov.pagopa.reward.notification.dto.controller.detail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportDetailFilter {

    // TODO for each exposed combination of filters, verify if an index is necessary
    private String status;
    private String cro;
}
