package it.gov.pagopa.reward.notification.service.exports.detail;

import it.gov.pagopa.reward.notification.dto.controller.*;
import it.gov.pagopa.reward.notification.dto.controller.detail.ExportDetailDTO;
import it.gov.pagopa.reward.notification.dto.controller.detail.ExportDetailFilter;
import it.gov.pagopa.reward.notification.dto.controller.detail.ExportPageDTO;
import it.gov.pagopa.reward.notification.dto.controller.detail.RefundDetailDTO;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ExportDetailService {

    Mono<ExportSummaryDTO> getExportSummary(String organizationId, String initiativeId, String exportId);

    Flux<ExportDetailDTO> getSingleExport(String organizationId, String initiativeId, String exportId, Pageable pageable, ExportDetailFilter filters);
    Mono<ExportPageDTO> getSingleExportPaged(String organizationId, String initiativeId, String exportId, Pageable pageable, ExportDetailFilter filters);
    Mono<ExportPageDTO> getExportDetailEmptyPage(Pageable pageable);


    Mono<RefundDetailDTO> getSingleRefundDetail(String organizationId, String initiativeId, String eventId);
}
