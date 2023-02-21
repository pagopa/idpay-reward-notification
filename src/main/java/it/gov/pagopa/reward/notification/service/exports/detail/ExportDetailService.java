package it.gov.pagopa.reward.notification.service.exports.detail;

import it.gov.pagopa.reward.notification.dto.controller.ExportSummaryDTO;
import reactor.core.publisher.Mono;

public interface ExportDetailService {

    Mono<ExportSummaryDTO> getExportSummary(String organizationId, String initiativeId, String exportId);
}
