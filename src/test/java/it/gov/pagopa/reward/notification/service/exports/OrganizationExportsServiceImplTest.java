package it.gov.pagopa.reward.notification.service.exports;

import it.gov.pagopa.reward.notification.dto.controller.ExportFilter;
import it.gov.pagopa.reward.notification.dto.controller.RewardExportsDTO;
import it.gov.pagopa.reward.notification.dto.mapper.RewardOrganizationExports2ExportsDTOMapper;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationExportsRepository;
import it.gov.pagopa.reward.notification.test.fakers.RewardExportsDTOFaker;
import it.gov.pagopa.reward.notification.test.fakers.RewardOrganizationExportsFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import reactor.core.publisher.Flux;

import java.util.List;

class OrganizationExportsServiceImplTest {

    @Autowired private RewardOrganizationExportsRepository rewardOrganizationExportsRepository;
    private final RewardOrganizationExports2ExportsDTOMapper rewardOrganizationExports2ExportsDTOMapper = new RewardOrganizationExports2ExportsDTOMapper();

    private OrganizationExportsService organizationExportsService;

    @BeforeEach
    void init() {
        organizationExportsService = new OrganizationExportsServiceImpl(rewardOrganizationExportsRepository, rewardOrganizationExports2ExportsDTOMapper);
    }

    @Test
    void testFindAllBy() {
        // Given
        RewardOrganizationExport rewardOrganizationExportMock = RewardOrganizationExportsFaker.mockInstance(1);

        Mockito.when(rewardOrganizationExportsRepository.findAllBy("ORGANIZATION_ID_1", "INITIATIVE_ID_1", PageRequest.of(0,2000), new ExportFilter())).thenReturn(Flux.just(rewardOrganizationExportMock));

        // When
        List<RewardExportsDTO> result = organizationExportsService.findAllBy(rewardOrganizationExportMock.getOrganizationId(), rewardOrganizationExportMock.getInitiativeId(), PageRequest.of(0,2000), new ExportFilter()).collectList().block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(rewardOrganizationExportMock.getId(), result.get(0).getId());
    }

    @Test
    void testFindAllPaged() {
        // Given
        RewardOrganizationExport rewardOrganizationExportMock = RewardOrganizationExportsFaker.mockInstance(1);
        RewardExportsDTO dtoMock = RewardExportsDTOFaker.mockInstance(1);
        Page<RewardExportsDTO> page = new PageImpl<>(List.of(dtoMock));

        Mockito.when(rewardOrganizationExportsRepository.findAllBy("ORGANIZATION_ID_1", "INITIATIVE_ID_1", PageRequest.of(0,2000), new ExportFilter())).thenReturn(Flux.just(rewardOrganizationExportMock));

        // When
        Page<RewardExportsDTO> result = organizationExportsService.findAllPaged(rewardOrganizationExportMock.getOrganizationId(), rewardOrganizationExportMock.getInitiativeId(), PageRequest.of(0,2000), new ExportFilter()).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(result, page);
    }

}