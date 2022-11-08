package it.gov.pagopa.reward.notification.controller;

import it.gov.pagopa.reward.notification.dto.controller.RewardExportsDTO;
import it.gov.pagopa.reward.notification.service.ErrorNotifierService;
import it.gov.pagopa.reward.notification.service.exports.OrganizationExportsServiceImpl;
import it.gov.pagopa.reward.notification.test.fakers.RewardExportsDTOFaker;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@WebFluxTest(controllers = {RewardExportsDTO.class})
@Import(NotificationControllerImpl.class)
class NotificationControllerImplTest {

    @MockBean
    OrganizationExportsServiceImpl organizationExportsService;

    @MockBean
    ErrorNotifierService errorNotifierService;

    @Autowired
    protected WebTestClient webClient;

    @Test
    void testGetExportsOk() {
        RewardExportsDTO rewardExportsDTOMock = RewardExportsDTOFaker.mockInstance(1);
        Mockito.when(organizationExportsService.findAllBy("ORGANIZATION_ID_1", "INITIATIVE_ID_1", null, null))
                .thenReturn(Flux.just(rewardExportsDTOMock));

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports")
                        .build(rewardExportsDTOMock.getOrganizationId(), rewardExportsDTOMock.getInitiativeId()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(RewardExportsDTO.class).isEqualTo(rewardExportsDTOMock);

        Mockito.verify(organizationExportsService, Mockito.times(1)).findAllBy("ORGANIZATION_ID_1", "INITIATIVE_ID_1", null, null);
    }

    @Test
    void testGetExportsEmpty() {
        RewardExportsDTO rewardExportsDTOMock = RewardExportsDTOFaker.mockInstance(1);
        Mockito.when(organizationExportsService.findAllBy("ORGANIZATION_ID_1", "INITIATIVE_ID_1", null, null))
                .thenReturn(Flux.empty());

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports")
                        .build(rewardExportsDTOMock.getOrganizationId(), rewardExportsDTOMock.getInitiativeId()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(List.class).isEqualTo(List.of());

        Mockito.verify(organizationExportsService, Mockito.times(1)).findAllBy("ORGANIZATION_ID_1", "INITIATIVE_ID_1", null, null);
    }

    @Test
    void testGetExportsCountOk() {
        Long count = 1L;
        Mockito.when(organizationExportsService.countAll("ORGANIZATION_ID_1", "INITIATIVE_ID_1", null, null))
                .thenReturn(Mono.just(count));

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports/count")
                        .build("ORGANIZATION_ID_1", "INITIATIVE_ID_1"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Long.class).isEqualTo(count);

        Mockito.verify(organizationExportsService, Mockito.times(1)).findAllBy("ORGANIZATION_ID_1", "INITIATIVE_ID_1", null, null);
    }

    @Test
    void testGetExportsCountEmpty() {
        Mockito.when(organizationExportsService.countAll("ORGANIZATION_ID_1", "INITIATIVE_ID_1", null, null))
                .thenReturn(Mono.just(0L));

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports/count")
                        .build("ORGANIZATION_ID_1", "INITIATIVE_ID_1"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Long.class).isEqualTo(0L);

        Mockito.verify(organizationExportsService, Mockito.times(1)).findAllBy("ORGANIZATION_ID_1", "INITIATIVE_ID_1", null, null);
    }

    @Test
    void testGetExportsPaged() {
        RewardExportsDTO dtoMock = RewardExportsDTOFaker.mockInstance(1);
        Page<RewardExportsDTO> page = new PageImpl<>(List.of(dtoMock));

        Mockito.when(organizationExportsService.findAllPaged("ORGANIZATION_ID_1", "INITIATIVE_ID_1", null, null))
                .thenReturn(Mono.just(page));

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports/paged")
                        .build("ORGANIZATION_ID_1", "INITIATIVE_ID_1"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Page.class).isEqualTo(page);

        Mockito.verify(organizationExportsService, Mockito.times(1)).findAllPaged("ORGANIZATION_ID_1", "INITIATIVE_ID_1", null, null);
    }
}