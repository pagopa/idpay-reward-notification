package it.gov.pagopa.reward.notification.service.imports;

import it.gov.pagopa.reward.notification.dto.controller.ExportFilter;
import it.gov.pagopa.reward.notification.dto.controller.ImportFilter;
import it.gov.pagopa.reward.notification.dto.controller.RewardExportsDTO;
import it.gov.pagopa.reward.notification.dto.controller.RewardImportsDTO;
import it.gov.pagopa.reward.notification.dto.mapper.RewardOrganizationImport2ImportsDTOMapper;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationImportsRepository;
import it.gov.pagopa.reward.notification.test.fakers.RewardOrganizationExportsFaker;
import it.gov.pagopa.reward.notification.test.fakers.RewardOrganizationImportFaker;
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
class OrganizationImportsServiceImplTest {

    @Mock
    private RewardOrganizationImportsRepository rewardOrganizationImportsRepository;
    private final RewardOrganizationImport2ImportsDTOMapper rewardOrganizationImport2ImportsDTOMapper = new RewardOrganizationImport2ImportsDTOMapper();

    private OrganizationImportsService organizationImportsService;

    @BeforeEach
    void init() {
        organizationImportsService = new OrganizationImportsServiceImpl(rewardOrganizationImportsRepository, rewardOrganizationImport2ImportsDTOMapper);
    }

    @Test
    void testFindAllBy() {
        // Given
        RewardOrganizationImport rewardOrganizationImport = RewardOrganizationImportFaker.mockInstance(1);

        Mockito.when(rewardOrganizationImportsRepository.findAllBy("orgId", "initiativeId", PageRequest.of(0,10), new ImportFilter())).thenReturn(Flux.just(rewardOrganizationImport));

        // When
        List<RewardImportsDTO> result = organizationImportsService.findAllBy(rewardOrganizationImport.getOrganizationId(), rewardOrganizationImport.getInitiativeId(), PageRequest.of(0,10), new ImportFilter()).collectList().block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals("reward-dispositive-1.zip", result.get(0).getFilePath());
    }

    @Test
    void testFindAllPaged() {
        // Given
        RewardOrganizationImport rewardOrganizationImport = RewardOrganizationImportFaker.mockInstance(1);
        RewardImportsDTO expectedResultDTO = rewardOrganizationImport2ImportsDTOMapper.apply(rewardOrganizationImport);
        PageRequest pageRequest = PageRequest.of(0,10);
        Page<RewardImportsDTO> expectedPage = new PageImpl<>(List.of(expectedResultDTO), pageRequest, 1);

        Mockito.when(rewardOrganizationImportsRepository.findAllBy("orgId", "initiativeId", pageRequest, new ImportFilter())).thenReturn(Flux.just(rewardOrganizationImport));
        Mockito.when(rewardOrganizationImportsRepository.countAll("orgId", "initiativeId", new ImportFilter())).thenReturn(Mono.just(1L));

        // When
        Page<RewardImportsDTO> result = organizationImportsService.findAllPaged(rewardOrganizationImport.getOrganizationId(), rewardOrganizationImport.getInitiativeId(), PageRequest.of(0,10), new ImportFilter()).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectedPage, result);
    }
}