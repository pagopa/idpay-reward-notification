package it.gov.pagopa.reward.notification.service.exports.detail;

import it.gov.pagopa.reward.notification.dto.controller.detail.*;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ExportDetailService {

    Mono<ExportSummaryDTO> getExportSummary(String organizationId, String initiativeId, String exportId);

    Flux<RewardNotificationDTO> getSingleExport(String organizationId, String initiativeId, String exportId, Pageable pageable, ExportDetailFilter filters);
    Mono<ExportContentPageDTO> getSingleExportPaged(String organizationId, String initiativeId, String exportId, Pageable pageable, ExportDetailFilter filters);
    Mono<ExportContentPageDTO> getExportDetailEmptyPage(Pageable pageable);


    Mono<RewardNotificationDetailDTO> getSingleRefundDetail(String organizationId, String initiativeId, String eventId);
}
