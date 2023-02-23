package it.gov.pagopa.reward.notification.service.exports.detail;

import it.gov.pagopa.reward.notification.dto.controller.detail.*;
import it.gov.pagopa.reward.notification.dto.mapper.detail.PageImpl2ExportPageDTOMapper;
import it.gov.pagopa.reward.notification.dto.mapper.detail.RewardOrganizationExport2ExportSummaryDTOMapper;
import it.gov.pagopa.reward.notification.dto.mapper.detail.RewardsNotification2DetailDTOMapper;
import it.gov.pagopa.reward.notification.dto.mapper.detail.RewardsNotification2ExportDetailDTOMapper;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationExportsRepository;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class ExportDetailServiceImpl implements ExportDetailService {

    //region repositories
    private final RewardOrganizationExportsRepository exportsRepository;
    private final RewardsNotificationRepository notificationRepository;
    //endregion

    //region mappers
    private final RewardOrganizationExport2ExportSummaryDTOMapper summaryMapper;
    private final RewardsNotification2ExportDetailDTOMapper exportDetailMapper;
    private final PageImpl2ExportPageDTOMapper pageMapper;
    private final RewardsNotification2DetailDTOMapper refundMapper;
    //endregion

    public ExportDetailServiceImpl(RewardOrganizationExportsRepository exportsRepository,
                                   RewardsNotificationRepository notificationRepository,
                                   RewardOrganizationExport2ExportSummaryDTOMapper summaryMapper,
                                   RewardsNotification2ExportDetailDTOMapper exportDetailMapper,
                                   PageImpl2ExportPageDTOMapper pageMapper,
                                   RewardsNotification2DetailDTOMapper refundMapper) {
        this.exportsRepository = exportsRepository;
        this.notificationRepository = notificationRepository;
        this.summaryMapper = summaryMapper;
        this.exportDetailMapper = exportDetailMapper;
        this.pageMapper = pageMapper;
        this.refundMapper = refundMapper;
    }


    @Override
    public Mono<ExportSummaryDTO> getExport(String organizationId, String initiativeId, String exportId) {
        return exportsRepository.findByIdAndOrganizationIdAndInitiativeId(organizationId, initiativeId, exportId)
                .map(summaryMapper);
    }

    @Override
    public Flux<RewardNotificationDTO> getExportNotifications(String exportId, String organizationId, String initiativeId, ExportDetailFilter filters, Pageable pageable) {
        return exportsRepository.findByIdAndOrganizationIdAndInitiativeId(exportId, organizationId, initiativeId)
                .flatMapMany(export ->
                        notificationRepository.findAll(export.getOrganizationId(), export.getInitiativeId(), export.getId(), filters, pageable)
                                .map(exportDetailMapper::apply)
                );
    }

    @Override
    public Mono<ExportContentPageDTO> getExportNotificationsPaged(String exportId, String organizationId, String initiativeId, ExportDetailFilter filters, Pageable pageable) {
        return exportsRepository.findByIdAndOrganizationIdAndInitiativeId(exportId, organizationId, initiativeId)
                .flatMapMany(export ->
                        notificationRepository.findAll(export.getOrganizationId(), export.getInitiativeId(), export.getId(), filters, pageable)
                                .map(exportDetailMapper::apply)
                )
                .collectList()
                .zipWith(notificationRepository.countAll(organizationId, initiativeId, exportId, filters, pageable))
                .map(t -> new PageImpl<>(t.getT1(), pageable, t.getT2()))
                .map(pageMapper::apply);
    }

    @Override
    public Mono<ExportContentPageDTO> getExportNotificationEmptyPage(Pageable pageable) {
        return Mono.just(pageable)
                .map(pageMapper::apply);
    }

    @Override
    public Mono<RewardNotificationDetailDTO> getRewardNotification(String notificationExternalId, String organizationId, String initiativeId) {
        log.info("[REWARD_NOTIFICATION][NOTIFICATION_DETAIL] Fetching notification detail with externalId {}", notificationExternalId);
        return notificationRepository.findByExternalIdAndOrganizationIdAndInitiativeId(notificationExternalId, organizationId, initiativeId)
                .map(refundMapper::apply);
    }
}
