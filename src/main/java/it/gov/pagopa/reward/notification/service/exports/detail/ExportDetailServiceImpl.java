package it.gov.pagopa.reward.notification.service.exports.detail;

import it.gov.pagopa.reward.notification.dto.controller.detail.*;
import it.gov.pagopa.reward.notification.dto.mapper.detail.PageImpl2ExportPageDTOMapper;
import it.gov.pagopa.reward.notification.dto.mapper.detail.RewardOrganizationExport2ExportSummaryDTOMapper;
import it.gov.pagopa.reward.notification.dto.mapper.detail.RewardsNotification2DetailDTOMapper;
import it.gov.pagopa.reward.notification.dto.mapper.detail.RewardsNotification2DTOMapper;
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
    private final RewardsNotification2DTOMapper exportDetailMapper;
    private final PageImpl2ExportPageDTOMapper pageMapper;
    private final RewardsNotification2DetailDTOMapper notificationDetailMapper;
    //endregion

    public ExportDetailServiceImpl(RewardOrganizationExportsRepository exportsRepository,
                                   RewardsNotificationRepository notificationRepository,
                                   RewardOrganizationExport2ExportSummaryDTOMapper summaryMapper,
                                   RewardsNotification2DTOMapper exportDetailMapper,
                                   PageImpl2ExportPageDTOMapper pageMapper,
                                   RewardsNotification2DetailDTOMapper notificationDetailMapper) {
        this.exportsRepository = exportsRepository;
        this.notificationRepository = notificationRepository;
        this.summaryMapper = summaryMapper;
        this.exportDetailMapper = exportDetailMapper;
        this.pageMapper = pageMapper;
        this.notificationDetailMapper = notificationDetailMapper;
    }


    @Override
    public Mono<ExportSummaryDTO> getExport(String exportId, String organizationId, String initiativeId) {
        log.info("[REWARD_NOTIFICATION][EXPORT_SUMMARY] Getting summary of export with exportId {}", exportId);

        return exportsRepository.findByIdAndOrganizationIdAndInitiativeId(exportId, organizationId, initiativeId)
                .map(summaryMapper);
    }

    @Override
    public Flux<RewardNotificationDTO> getExportNotifications(String exportId, String organizationId, String initiativeId, ExportDetailFilter filters, Pageable pageable) {
        log.info("[REWARD_NOTIFICATION][EXPORT_NOTIFICATIONS] Fetching notifications of export with exportId {} and following filters: {}", exportId, filters);

        return exportsRepository.findByIdAndOrganizationIdAndInitiativeId(exportId, organizationId, initiativeId)
                .flatMapMany(export ->
                        notificationRepository.findAll(export.getId(), export.getOrganizationId(), export.getInitiativeId(), filters, pageable)
                                .map(exportDetailMapper::apply)
                );
    }

    @Override
    public Mono<ExportContentPageDTO> getExportNotificationsPaged(String exportId, String organizationId, String initiativeId, ExportDetailFilter filters, Pageable pageable) {
        log.info("[REWARD_NOTIFICATION][EXPORT_NOTIFICATIONS] Fetching notifications of export with exportId {} and following filters: {}", exportId, filters);

        return exportsRepository.findByIdAndOrganizationIdAndInitiativeId(exportId, organizationId, initiativeId)
                .flatMapMany(export ->
                        notificationRepository.findAll(export.getId(), export.getOrganizationId(), export.getInitiativeId(), filters, pageable)
                                .map(exportDetailMapper::apply)
                )
                .collectList()
                .zipWith(notificationRepository.countAll(exportId, organizationId, initiativeId, filters))
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
                .map(n -> {
                    log.info("[REWARD_NOTIFICATION][NOTIFICATION_DETAIL] Found notification with externalId {}", n.getExternalId());

                    return notificationDetailMapper.apply(n);
                });
    }
}
