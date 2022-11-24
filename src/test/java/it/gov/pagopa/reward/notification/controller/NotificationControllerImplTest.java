package it.gov.pagopa.reward.notification.controller;

import it.gov.pagopa.reward.notification.config.JsonConfig;
import it.gov.pagopa.reward.notification.dto.controller.ExportFilter;
import it.gov.pagopa.reward.notification.dto.controller.FeedbackImportFilter;
import it.gov.pagopa.reward.notification.dto.controller.RewardExportsDTO;
import it.gov.pagopa.reward.notification.dto.controller.RewardImportsDTO;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.service.csv.out.ExportRewardNotificationCsvService;
import it.gov.pagopa.reward.notification.service.exports.OrganizationExportsServiceImpl;
import it.gov.pagopa.reward.notification.service.imports.OrganizationImportsServiceImpl;
import it.gov.pagopa.reward.notification.test.fakers.RewardExportsDTOFaker;
import it.gov.pagopa.reward.notification.test.fakers.RewardImportsDTOFaker;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@WebFluxTest(controllers = {NotificationController.class})
@Import(JsonConfig.class)
class NotificationControllerImplTest {

    @MockBean
    private OrganizationExportsServiceImpl organizationExportsServiceMock;
    @MockBean
    private ExportRewardNotificationCsvService exportRewardNotificationCsvServiceMock;
    @MockBean
    private OrganizationImportsServiceImpl organizationImportsServiceMock;

    @Autowired
    protected WebTestClient webClient;

    private static final PageRequest TEST_PAGE_REQUEST = PageRequest.of(0,10);

    @Test
    void testforceExportScheduling() {
        Mockito.when(exportRewardNotificationCsvServiceMock.execute())
                .thenReturn(Flux.empty());

        webClient.get()
                .uri("/idpay/reward/notification/exports/start")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RewardOrganizationExport.class).isEqualTo(Collections.emptyList());

        Mockito.verify(exportRewardNotificationCsvServiceMock).execute();
    }

    @Test
    void testGetExportsOk() {
        RewardExportsDTO rewardExportsDTOMock = RewardExportsDTOFaker.mockInstance(1);

        Mockito.when(organizationExportsServiceMock.findAllBy("ORGANIZATION_ID_1", "INITIATIVE_ID_1", TEST_PAGE_REQUEST, new ExportFilter()))
                .thenReturn(Flux.just(rewardExportsDTOMock));

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports")
                        .build(rewardExportsDTOMock.getOrganizationId(), rewardExportsDTOMock.getInitiativeId()))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RewardExportsDTO.class).isEqualTo(List.of(rewardExportsDTOMock));

        Mockito.verify(organizationExportsServiceMock, Mockito.times(1)).findAllBy("ORGANIZATION_ID_1", "INITIATIVE_ID_1", TEST_PAGE_REQUEST, new ExportFilter());
    }

    @Test
    void testGetExportsEmpty() {
        RewardExportsDTO rewardExportsDTOMock = RewardExportsDTOFaker.mockInstance(1);
        Mockito.when(organizationExportsServiceMock.findAllBy("ORGANIZATION_ID_1", "INITIATIVE_ID_1", TEST_PAGE_REQUEST, new ExportFilter()))
                .thenReturn(Flux.empty());

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports")
                        .build(rewardExportsDTOMock.getOrganizationId(), rewardExportsDTOMock.getInitiativeId()))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RewardExportsDTO.class).isEqualTo(Collections.emptyList());

        Mockito.verify(organizationExportsServiceMock, Mockito.times(1)).findAllBy("ORGANIZATION_ID_1", "INITIATIVE_ID_1", TEST_PAGE_REQUEST, new ExportFilter());
    }

    @Test
    void testGetExportsCountOk() {
        Long count = 1L;
        Mockito.when(organizationExportsServiceMock.countAll("ORGANIZATION_ID_1", "INITIATIVE_ID_1", new ExportFilter()))
                .thenReturn(Mono.just(count));

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports/count")
                        .build("ORGANIZATION_ID_1", "INITIATIVE_ID_1"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Long.class).isEqualTo(count);

        Mockito.verify(organizationExportsServiceMock, Mockito.times(1)).countAll("ORGANIZATION_ID_1", "INITIATIVE_ID_1", new ExportFilter());
    }

    @Test
    void testGetExportsCountEmpty() {
        Mockito.when(organizationExportsServiceMock.countAll("ORGANIZATION_ID_1", "INITIATIVE_ID_1", new ExportFilter()))
                .thenReturn(Mono.empty());

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports/count")
                        .build("ORGANIZATION_ID_1", "INITIATIVE_ID_1"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Long.class).isEqualTo(0L);

        Mockito.verify(organizationExportsServiceMock, Mockito.times(1)).countAll("ORGANIZATION_ID_1", "INITIATIVE_ID_1", new ExportFilter());
    }

    @Test
    void testGetExportsPaged() {
        RewardExportsDTO dtoMock = RewardExportsDTOFaker.mockInstance(1);
        PageImpl<RewardExportsDTO> pageMock = new PageImpl<>(List.of(dtoMock), TEST_PAGE_REQUEST, 1);


        Mockito.when(organizationExportsServiceMock.findAllPaged("ORGANIZATION_ID_1", "INITIATIVE_ID_1", TEST_PAGE_REQUEST, new ExportFilter()))
                .thenReturn(Mono.just(pageMock));

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports/paged")
                        .build("ORGANIZATION_ID_1", "INITIATIVE_ID_1"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Page<RewardExportsDTO>>() {})
                .isEqualTo(pageMock);

        Mockito.verify(organizationExportsServiceMock, Mockito.times(1)).findAllPaged("ORGANIZATION_ID_1", "INITIATIVE_ID_1", TEST_PAGE_REQUEST, new ExportFilter());
    }

    @Test
    void testGetImportsOk() {
        RewardImportsDTO rewardImportsDTO = RewardImportsDTOFaker.mockInstance(1);

        Mockito.when(organizationImportsServiceMock.findAllBy("ORGANIZATION_ID_1", "INITIATIVE_ID_1", TEST_PAGE_REQUEST, new FeedbackImportFilter()))
                .thenReturn(Flux.just(rewardImportsDTO));

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/imports")
                        .build(rewardImportsDTO.getOrganizationId(), rewardImportsDTO.getInitiativeId()))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RewardImportsDTO.class).isEqualTo(List.of(rewardImportsDTO));

        Mockito.verify(organizationImportsServiceMock, Mockito.times(1)).findAllBy("ORGANIZATION_ID_1", "INITIATIVE_ID_1", TEST_PAGE_REQUEST, new FeedbackImportFilter());
    }

    @Test
    void testGetImportsEmpty() {
        RewardImportsDTO rewardImportsDTO = RewardImportsDTOFaker.mockInstance(1);
        Mockito.when(organizationImportsServiceMock.findAllBy("ORGANIZATION_ID_1", "INITIATIVE_ID_1", TEST_PAGE_REQUEST, new FeedbackImportFilter()))
                .thenReturn(Flux.empty());

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/imports")
                        .build(rewardImportsDTO.getOrganizationId(), rewardImportsDTO.getInitiativeId()))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RewardExportsDTO.class).isEqualTo(Collections.emptyList());

        Mockito.verify(organizationImportsServiceMock, Mockito.times(1)).findAllBy("ORGANIZATION_ID_1", "INITIATIVE_ID_1", TEST_PAGE_REQUEST, new FeedbackImportFilter());
    }

    @Test
    void testGetImportsCountOk() {
        Long count = 1L;
        Mockito.when(organizationImportsServiceMock.countAll("ORGANIZATION_ID_1", "INITIATIVE_ID_1", new FeedbackImportFilter()))
                .thenReturn(Mono.just(count));

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/imports/count")
                        .build("ORGANIZATION_ID_1", "INITIATIVE_ID_1"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Long.class).isEqualTo(count);

        Mockito.verify(organizationImportsServiceMock, Mockito.times(1)).countAll("ORGANIZATION_ID_1", "INITIATIVE_ID_1", new FeedbackImportFilter());
    }

    @Test
    void testGetImportsCountEmpty() {
        Mockito.when(organizationImportsServiceMock.countAll("ORGANIZATION_ID_1", "INITIATIVE_ID_1", new FeedbackImportFilter()))
                .thenReturn(Mono.empty());

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/imports/count")
                        .build("ORGANIZATION_ID_1", "INITIATIVE_ID_1"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Long.class).isEqualTo(0L);

        Mockito.verify(organizationImportsServiceMock, Mockito.times(1)).countAll("ORGANIZATION_ID_1", "INITIATIVE_ID_1", new FeedbackImportFilter());
    }

    @Test
    void testGetImportsPaged() {
        RewardImportsDTO rewardImportsDTO = RewardImportsDTOFaker.mockInstance(1);
        PageImpl<RewardImportsDTO> pageMock = new PageImpl<>(List.of(rewardImportsDTO), TEST_PAGE_REQUEST, 1);


        Mockito.when(organizationImportsServiceMock.findAllPaged("ORGANIZATION_ID_1", "INITIATIVE_ID_1", TEST_PAGE_REQUEST, new FeedbackImportFilter()))
                .thenReturn(Mono.just(pageMock));

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/imports/paged")
                        .build("ORGANIZATION_ID_1", "INITIATIVE_ID_1"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Page<RewardImportsDTO>>() {})
                .isEqualTo(pageMock);

        Mockito.verify(organizationImportsServiceMock, Mockito.times(1)).findAllPaged("ORGANIZATION_ID_1", "INITIATIVE_ID_1", TEST_PAGE_REQUEST, new FeedbackImportFilter());
    }

    @Test
    void testGetImportErrorsCsvOk() {
        String expectedCsvString = "";

        Mockito.when(organizationImportsServiceMock.getErrorsCsvByImportId("orgId", "initiativeId", "reward-dispositive-1.zip"))
                .thenReturn(Mono.just(expectedCsvString));

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/imports/{importId}/errors")
                        .build("orgId", "initiativeId", "reward-dispositive-1.zip"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo(expectedCsvString);

        Mockito.verify(organizationImportsServiceMock, Mockito.times(1)).getErrorsCsvByImportId("orgId", "initiativeId", "reward-dispositive-1.zip");
    }

    @Test
    void testGetImportErrorsEmpty() {
        Mockito.when(organizationImportsServiceMock.getErrorsCsvByImportId("orgId", "initiativeId", "reward-dispositive-1.zip"))
                .thenReturn(Mono.empty());

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/imports/{importId}/errors")
                        .build("orgId", "initiativeId", "reward-dispositive-1.zip"))
                .exchange()
                .expectStatus().isNotFound();

        Mockito.verify(organizationImportsServiceMock, Mockito.times(1)).getErrorsCsvByImportId("orgId", "initiativeId", "reward-dispositive-1.zip");
    }
}