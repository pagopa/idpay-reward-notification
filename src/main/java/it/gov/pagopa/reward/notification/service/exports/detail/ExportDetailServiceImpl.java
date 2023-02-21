package it.gov.pagopa.reward.notification.service.exports.detail;

import it.gov.pagopa.reward.notification.dto.controller.ExportSummaryDTO;
import it.gov.pagopa.reward.notification.dto.mapper.RewardOrganizationExport2ExportSummaryDTOMapper;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationExportsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class ExportDetailServiceImpl implements ExportDetailService {

    private final RewardOrganizationExportsRepository exportsRepository;
    private final RewardOrganizationExport2ExportSummaryDTOMapper summaryMapper;

    public ExportDetailServiceImpl(RewardOrganizationExportsRepository exportsRepository, RewardOrganizationExport2ExportSummaryDTOMapper summaryMapper) {
        this.exportsRepository = exportsRepository;
        this.summaryMapper = summaryMapper;
    }

    @Override
    public Mono<ExportSummaryDTO> getExportSummary(String organizationId, String initiativeId, String exportId) {
        return exportsRepository.findByOrganizationIdAndInitiativeIdAndId(organizationId, initiativeId, exportId)
                .map(summaryMapper);
    }
}
