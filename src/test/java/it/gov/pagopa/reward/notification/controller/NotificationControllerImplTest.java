package it.gov.pagopa.reward.notification.controller;

import it.gov.pagopa.reward.notification.dto.controller.RewardExportsDTO;
import it.gov.pagopa.reward.notification.exception.ClientExceptionNoBody;
import it.gov.pagopa.reward.notification.service.ErrorNotifierService;
import it.gov.pagopa.reward.notification.service.exports.OrganizationExportsServiceImpl;
import it.gov.pagopa.reward.notification.test.fakers.RewardExportsDTOFaker;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
        Mockito.when(organizationExportsService.findAllBy(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Flux.just(rewardExportsDTOMock));

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/exports")
                        .build(rewardExportsDTOMock.getOrganizationId(), rewardExportsDTOMock.getInitiativeId()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(RewardExportsDTO.class).isEqualTo(rewardExportsDTOMock);

        Mockito.verify(organizationExportsService, Mockito.times(1)).findAllBy(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void testGetExportsNotFound() {
        RewardExportsDTO rewardExportsDTOMock = RewardExportsDTOFaker.mockInstance(1);
        Mockito.when(organizationExportsService.findAllBy(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Flux.error(new ClientExceptionNoBody(HttpStatus.NOT_FOUND)));

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/exports")
                        .build(rewardExportsDTOMock.getOrganizationId(), rewardExportsDTOMock.getInitiativeId()))
                .exchange()
                .expectStatus().isNotFound();

        Mockito.verify(organizationExportsService, Mockito.times(1)).findAllBy(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void testGetExportsCountOk() {
        Long count = 1L;
        Mockito.when(organizationExportsService.countAll(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(count));

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/exports/count")
                        .build("ORG", "INTV"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Long.class).isEqualTo(count);

        Mockito.verify(organizationExportsService, Mockito.times(1)).findAllBy(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void testGetExportsCountNotFound() {
        Long count = 1L;
        Mockito.when(organizationExportsService.countAll(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.error(new ClientExceptionNoBody(HttpStatus.NOT_FOUND)));

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/exports/count")
                        .build("ORG", "INTV"))
                .exchange()
                .expectStatus().isNotFound();

        Mockito.verify(organizationExportsService, Mockito.times(1)).findAllBy(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    /*@Test
    void testGetExportsPaged() {
        Page<RewardExportsDTO> pageMock = Mockito.mock(Page.class);

    }*/
}