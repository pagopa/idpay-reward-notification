package it.gov.pagopa.reward.notification.service.imports;

import it.gov.pagopa.reward.notification.dto.controller.FeedbackImportFilter;
import it.gov.pagopa.reward.notification.dto.controller.RewardImportsDTO;
import it.gov.pagopa.reward.notification.dto.mapper.RewardOrganizationImport2ImportsDTOMapper;
import it.gov.pagopa.reward.notification.dto.mapper.RewardOrganizationImportErrors2ErrorsCsvMapper;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationImportsRepository;
import it.gov.pagopa.reward.notification.service.csv.out.writer.FeedbackImportErrorsCsvService;
import it.gov.pagopa.reward.notification.service.csv.out.writer.FeedbackImportErrorsCsvServiceImpl;
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
    private RewardOrganizationImportsRepository rewardOrganizationImportsRepositoryMock;
    private final FeedbackImportErrorsCsvService feedbackImportErrorsCsvService = new FeedbackImportErrorsCsvServiceImpl(';');
    private final RewardOrganizationImport2ImportsDTOMapper rewardOrganizationImport2ImportsDTOMapper = new RewardOrganizationImport2ImportsDTOMapper();
    private final RewardOrganizationImportErrors2ErrorsCsvMapper errors2ErrorsCsvMapper = new RewardOrganizationImportErrors2ErrorsCsvMapper();

    private OrganizationImportsService organizationImportsService;

    @BeforeEach
    void init() {
        organizationImportsService = new OrganizationImportsServiceImpl(rewardOrganizationImportsRepositoryMock, rewardOrganizationImport2ImportsDTOMapper, feedbackImportErrorsCsvService, errors2ErrorsCsvMapper);
    }

    @Test
    void testFindAllBy() {
        // Given
        RewardOrganizationImport rewardOrganizationImport = RewardOrganizationImportFaker.mockInstance(1);

        Mockito.when(rewardOrganizationImportsRepositoryMock.findAllBy("orgId", "initiativeId", PageRequest.of(0,10), new FeedbackImportFilter())).thenReturn(Flux.just(rewardOrganizationImport));

        // When
        List<RewardImportsDTO> result = organizationImportsService.findAllBy(rewardOrganizationImport.getOrganizationId(), rewardOrganizationImport.getInitiativeId(), PageRequest.of(0,10), new FeedbackImportFilter()).collectList().block();

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

        Mockito.when(rewardOrganizationImportsRepositoryMock.findAllBy("orgId", "initiativeId", pageRequest, new FeedbackImportFilter())).thenReturn(Flux.just(rewardOrganizationImport));
        Mockito.when(rewardOrganizationImportsRepositoryMock.countAll("orgId", "initiativeId", new FeedbackImportFilter())).thenReturn(Mono.just(1L));

        // When
        Page<RewardImportsDTO> result = organizationImportsService.findAllPaged(rewardOrganizationImport.getOrganizationId(), rewardOrganizationImport.getInitiativeId(), PageRequest.of(0,10), new FeedbackImportFilter()).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectedPage, result);
    }

    @Test
    void testGetErrors() {
        // Given
        RewardOrganizationImport rewardOrganizationImport = RewardOrganizationImportFaker.mockInstance(1);
        rewardOrganizationImport.setErrors(List.of(
                new RewardOrganizationImport.RewardOrganizationImportError(1, "CODE", "ERROR")
        ));

        Mockito.when(rewardOrganizationImportsRepositoryMock.findByImportId(
                rewardOrganizationImport.getOrganizationId(),
                rewardOrganizationImport.getInitiativeId(),
                rewardOrganizationImport.getFilePath())
        ).thenReturn(Mono.just(rewardOrganizationImport));

        // When
        String result = organizationImportsService.getErrorsCsvByImportId(
                rewardOrganizationImport.getOrganizationId(),
                rewardOrganizationImport.getInitiativeId(),
                rewardOrganizationImport.getFilePath()
        ).block();

        // Then
        String expected = "\"row\";\"errorCode\";\"errorDescription\"\n\"1\";\"CODE\";\"ERROR\"\n";
        Assertions.assertNotNull(result);
        Assertions.assertEquals(expected, result);

    }
}