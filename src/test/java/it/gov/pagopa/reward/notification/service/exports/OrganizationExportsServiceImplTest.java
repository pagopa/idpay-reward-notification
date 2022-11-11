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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class OrganizationExportsServiceImplTest {

    @Mock
    private RewardOrganizationExportsRepository rewardOrganizationExportsRepositoryMock;
    private final RewardOrganizationExports2ExportsDTOMapper rewardOrganizationExports2ExportsDTOMapper = new RewardOrganizationExports2ExportsDTOMapper();

    private OrganizationExportsService organizationExportsService;

    @BeforeEach
    void init() {
        organizationExportsService = new OrganizationExportsServiceImpl(rewardOrganizationExportsRepositoryMock, rewardOrganizationExports2ExportsDTOMapper);
    }

    @Test
    void testFindAllBy() {
        // Given
        RewardOrganizationExport rewardOrganizationExportMock = RewardOrganizationExportsFaker.mockInstance(1);

        Mockito.when(rewardOrganizationExportsRepositoryMock.findAllBy("ORGANIZATION_ID_1", "INITIATIVE_ID_1", PageRequest.of(0,10), new ExportFilter())).thenReturn(Flux.just(rewardOrganizationExportMock));

        // When
        List<RewardExportsDTO> result = organizationExportsService.findAllBy(rewardOrganizationExportMock.getOrganizationId(), rewardOrganizationExportMock.getInitiativeId(), PageRequest.of(0,10), new ExportFilter()).collectList().block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(rewardOrganizationExportMock.getId(), result.get(0).getId());
    }

    @Test
    void testFindAllPaged() {
        // Given
        RewardOrganizationExport rewardOrganizationExport = RewardOrganizationExportsFaker.mockInstance(1);
        RewardExportsDTO expectedResultDTO = rewardOrganizationExports2ExportsDTOMapper.apply(rewardOrganizationExport);
        PageRequest pageRequest = PageRequest.of(0,10);
        Page<RewardExportsDTO> expectedPage = new PageImpl<>(List.of(expectedResultDTO), pageRequest, 1);

        Mockito.when(rewardOrganizationExportsRepositoryMock.findAllBy("ORGANIZATION_ID_1", "INITIATIVE_ID_1", pageRequest, new ExportFilter())).thenReturn(Flux.just(rewardOrganizationExport));
        Mockito.when(rewardOrganizationExportsRepositoryMock.countAll("ORGANIZATION_ID_1", "INITIATIVE_ID_1", new ExportFilter())).thenReturn(Mono.just(1L));

        // When
        Page<RewardExportsDTO> result = organizationExportsService.findAllPaged(rewardOrganizationExport.getOrganizationId(), rewardOrganizationExport.getInitiativeId(), PageRequest.of(0,10), new ExportFilter()).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectedPage, result);
    }

}