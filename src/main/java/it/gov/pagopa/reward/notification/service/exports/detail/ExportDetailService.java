package it.gov.pagopa.reward.notification.service.exports.detail;

import it.gov.pagopa.reward.notification.dto.controller.detail.*;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ExportDetailService {

    Mono<ExportSummaryDTO> getExport(String exportId, String organizationId, String initiativeId);

    Flux<RewardNotificationDTO> getExportNotifications(String exportId, String organizationId, String initiativeId, ExportDetailFilter filters, Pageable pageable);
    Mono<ExportContentPageDTO> getExportNotificationsPaged(String exportId, String organizationId, String initiativeId, ExportDetailFilter filters, Pageable pageable);
    Mono<ExportContentPageDTO> getExportNotificationEmptyPage(Pageable pageable);


    Mono<RewardNotificationDetailDTO> getRewardNotification(String notificationExternalId, String organizationId, String initiativeId);
}
