package it.gov.pagopa.reward.notification.controller;

import it.gov.pagopa.reward.notification.config.JsonConfig;
import it.gov.pagopa.reward.notification.dto.controller.ExportFilter;
import it.gov.pagopa.reward.notification.dto.controller.RewardExportsDTO;
import it.gov.pagopa.reward.notification.service.exports.OrganizationExportsServiceImpl;
import it.gov.pagopa.reward.notification.test.fakers.RewardExportsDTOFaker;
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
    OrganizationExportsServiceImpl organizationExportsServiceMock;

    @Autowired
    protected WebTestClient webClient;

    @Test
    void testGetExportsOk() {
        RewardExportsDTO rewardExportsDTOMock = RewardExportsDTOFaker.mockInstance(1);

        Mockito.when(organizationExportsServiceMock.findAllBy("ORGANIZATION_ID_1", "INITIATIVE_ID_1", PageRequest.of(0, 2000), new ExportFilter()))
                .thenReturn(Flux.just(rewardExportsDTOMock));

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports")
                        .build(rewardExportsDTOMock.getOrganizationId(), rewardExportsDTOMock.getInitiativeId()))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RewardExportsDTO.class).isEqualTo(List.of(rewardExportsDTOMock));

        Mockito.verify(organizationExportsServiceMock, Mockito.times(1)).findAllBy("ORGANIZATION_ID_1", "INITIATIVE_ID_1", PageRequest.of(0, 2000), new ExportFilter());
    }

    @Test
    void testGetExportsEmpty() {
        RewardExportsDTO rewardExportsDTOMock = RewardExportsDTOFaker.mockInstance(1);
        Mockito.when(organizationExportsServiceMock.findAllBy("ORGANIZATION_ID_1", "INITIATIVE_ID_1", PageRequest.of(0, 2000), new ExportFilter()))
                .thenReturn(Flux.empty());

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports")
                        .build(rewardExportsDTOMock.getOrganizationId(), rewardExportsDTOMock.getInitiativeId()))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RewardExportsDTO.class).isEqualTo(Collections.emptyList());

        Mockito.verify(organizationExportsServiceMock, Mockito.times(1)).findAllBy("ORGANIZATION_ID_1", "INITIATIVE_ID_1", PageRequest.of(0, 2000), new ExportFilter());
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
        PageRequest pageRequest = PageRequest.of(0, 2000);
        PageImpl<RewardExportsDTO> pageMock = new PageImpl<>(List.of(dtoMock), pageRequest, 1);


        Mockito.when(organizationExportsServiceMock.findAllPaged("ORGANIZATION_ID_1", "INITIATIVE_ID_1", pageRequest, new ExportFilter()))
                .thenReturn(Mono.just(pageMock));

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports/paged")
                        .build("ORGANIZATION_ID_1", "INITIATIVE_ID_1"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Page<RewardExportsDTO>>() {})
                .isEqualTo(pageMock);

        Mockito.verify(organizationExportsServiceMock, Mockito.times(1)).findAllPaged("ORGANIZATION_ID_1", "INITIATIVE_ID_1", pageRequest, new ExportFilter());
    }
}