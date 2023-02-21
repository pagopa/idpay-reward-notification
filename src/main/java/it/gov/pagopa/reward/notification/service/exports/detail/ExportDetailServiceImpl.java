package it.gov.pagopa.reward.notification.service.exports.detail;

import it.gov.pagopa.reward.notification.dto.controller.*;
import it.gov.pagopa.reward.notification.dto.mapper.detail.PageImpl2ExportPageDTOMapper;
import it.gov.pagopa.reward.notification.dto.mapper.detail.RewardOrganizationExport2ExportSummaryDTOMapper;
import it.gov.pagopa.reward.notification.dto.mapper.detail.RewardsNotification2ExportDetailDTOMapper;
import it.gov.pagopa.reward.notification.dto.mapper.detail.RewardsNotification2RefundDetailDTOMapper;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationExportsRepository;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.csv.out.retrieve.User2NotifyRetrieverService;
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
    private final RewardsNotification2RefundDetailDTOMapper refundMapper;
    //endregion

    private final User2NotifyRetrieverService userService;

    public ExportDetailServiceImpl(RewardOrganizationExportsRepository exportsRepository,
                                   RewardsNotificationRepository notificationRepository,
                                   RewardOrganizationExport2ExportSummaryDTOMapper summaryMapper,
                                   RewardsNotification2ExportDetailDTOMapper exportDetailMapper,
                                   PageImpl2ExportPageDTOMapper pageMapper,
                                   RewardsNotification2RefundDetailDTOMapper refundMapper,
                                   User2NotifyRetrieverService userService) {
        this.exportsRepository = exportsRepository;
        this.notificationRepository = notificationRepository;
        this.summaryMapper = summaryMapper;
        this.exportDetailMapper = exportDetailMapper;
        this.pageMapper = pageMapper;
        this.refundMapper = refundMapper;
        this.userService = userService;
    }


    @Override
    public Mono<ExportSummaryDTO> getExportSummary(String organizationId, String initiativeId, String exportId) {
        return exportsRepository.findByOrganizationIdAndInitiativeIdAndId(organizationId, initiativeId, exportId)
                .map(summaryMapper);
    }

    @Override
    public Flux<ExportDetailDTO> getSingleExport(String organizationId, String initiativeId, String exportId, Pageable pageable, ExportDetailFilter filters) {
        return exportsRepository.findByOrganizationIdAndInitiativeIdAndId(organizationId, initiativeId, exportId)
                .flatMapMany(export ->
                        notificationRepository.findAllWithFilters(export.getOrganizationId(), export.getInitiativeId(), export.getId(), pageable, filters)
                                .map(exportDetailMapper::apply)
                );
    }

    @Override
    public Mono<ExportPageDTO> getSingleExportPaged(String organizationId, String initiativeId, String exportId, Pageable pageable, ExportDetailFilter filters) {
        return exportsRepository.findByOrganizationIdAndInitiativeIdAndId(organizationId, initiativeId, exportId)
                .flatMapMany(export ->
                        notificationRepository.findAllWithFilters(export.getOrganizationId(), export.getInitiativeId(), export.getId(), pageable, filters)
                                .map(exportDetailMapper::apply)
                )
                .collectList()
                .zipWith(notificationRepository.countAll(organizationId, initiativeId, exportId, pageable, filters))
                .map(t -> new PageImpl<>(t.getT1(), pageable, t.getT2()))
                .map(pageMapper::apply);
    }

    @Override
    public Mono<ExportPageDTO> getExportDetailEmptyPage(Pageable pageable) {
        return Mono.just(pageable)
                .map(pageMapper::apply);
    }

    @Override
    public Mono<RefundDetailDTO> getSingleRefundDetail(String organizationId, String initiativeId, String exportId, String eventId) {
        return notificationRepository.findByOrganizationIdAndInitiativeIdAndExportIdAndExternalId(organizationId, initiativeId, exportId, eventId)
                .flatMap(userService::retrieveUser)
                .map(p -> refundMapper.apply(p.getLeft(), p.getRight()));
    }
}
