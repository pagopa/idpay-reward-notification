package it.gov.pagopa.reward.notification.controller;

import it.gov.pagopa.reward.notification.config.JsonConfig;
import it.gov.pagopa.reward.notification.dto.controller.ExportFilter;
import it.gov.pagopa.reward.notification.dto.controller.FeedbackImportFilter;
import it.gov.pagopa.reward.notification.dto.controller.RewardExportsDTO;
import it.gov.pagopa.reward.notification.dto.controller.RewardImportsDTO;
import it.gov.pagopa.reward.notification.dto.controller.detail.*;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardSuspendedUser;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.service.RewardsNotificationExpiredInitiativeHandlerService;
import it.gov.pagopa.reward.notification.service.csv.out.ExportRewardNotificationCsvService;
import it.gov.pagopa.reward.notification.service.exports.OrganizationExportsServiceImpl;
import it.gov.pagopa.reward.notification.service.exports.detail.ExportDetailService;
import it.gov.pagopa.reward.notification.service.imports.OrganizationImportsServiceImpl;
import it.gov.pagopa.reward.notification.service.suspension.UserSuspensionServiceImpl;
import it.gov.pagopa.reward.notification.test.fakers.*;
import it.gov.pagopa.reward.notification.utils.AuditUtilities;
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
import org.springframework.http.ContentDisposition;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@WebFluxTest(controllers = {NotificationController.class})
@Import(JsonConfig.class)
class NotificationControllerImplTest {

    private static final LocalDate NOW = LocalDate.now();

    @MockBean
    private OrganizationExportsServiceImpl organizationExportsServiceMock;
    @MockBean
    private ExportRewardNotificationCsvService exportRewardNotificationCsvServiceMock;
    @MockBean
    private OrganizationImportsServiceImpl organizationImportsServiceMock;
    @MockBean
    private RewardsNotificationExpiredInitiativeHandlerService expiredInitiativeHandlerService;
    @MockBean
    private ExportDetailService exportDetailServiceMock;
    @MockBean
    private AuditUtilities auditUtilities;
    @MockBean
    private UserSuspensionServiceImpl userSuspensionServiceMock;
    @Autowired
    protected WebTestClient webClient;

    private static final PageRequest TEST_PAGE_REQUEST = PageRequest.of(0, 10);

    @Test
    void testforceExportScheduling() {
        Mockito.when(exportRewardNotificationCsvServiceMock.execute(NOW))
                .thenReturn(Flux.empty());

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/reward/notification/exports/start")
                        .queryParam("notificationDateToSearch", NOW)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RewardOrganizationExport.class).isEqualTo(Collections.emptyList());

        Mockito.verify(exportRewardNotificationCsvServiceMock).execute(NOW);
    }

    @Test
    void testForceExpiredInitiativesScheduling() {
        Mockito.when(expiredInitiativeHandlerService.handle())
                .thenReturn(Flux.empty());

        webClient.get()
                .uri("/idpay/reward/notification/expired-initiatives/start")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RewardsNotification.class).isEqualTo(Collections.emptyList());

        Mockito.verify(expiredInitiativeHandlerService).handle();
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
                .expectBody(new ParameterizedTypeReference<Page<RewardExportsDTO>>() {
                })
                .isEqualTo(pageMock);

        Mockito.verify(organizationExportsServiceMock, Mockito.times(1)).findAllPaged("ORGANIZATION_ID_1", "INITIATIVE_ID_1", TEST_PAGE_REQUEST, new ExportFilter());
    }

    @Test
    void testGetExportOk() {
        ExportSummaryDTO dto = ExportSummaryDTOFaker.mockInstance(1);

        Mockito.when(exportDetailServiceMock.getExport("EXPORTID1", "ORGANIZATIONID1", "INITIATIVEID1")).thenReturn(Mono.just(dto));

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports/{exportId}")
                        .build("ORGANIZATIONID1", "INITIATIVEID1", "EXPORTID1"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ExportSummaryDTO.class)
                .isEqualTo(dto);
    }

    @Test
    void testGetExportEmpty() {
        Mockito.when(exportDetailServiceMock.getExport("EXPORTID1", "ORGANIZATIONID1", "INITIATIVEID1")).thenReturn(Mono.empty());

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports/{exportId}")
                        .build("ORGANIZATIONID1", "INITIATIVEID1", "EXPORTID1"))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void testGetExportNotificationsOk() {
        List<RewardNotificationDTO> dto = List.of(RewardNotificationDTOFaker.mockInstance(1));

        Mockito.when(exportDetailServiceMock.getExportNotifications("EXPORTID1", "ORGANIZATIONID1", "INITIATIVEID1", new ExportDetailFilter(), TEST_PAGE_REQUEST))
                .thenReturn(Flux.fromIterable(dto));

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports/{exportId}/content")
                        .build("ORGANIZATIONID1", "INITIATIVEID1", "EXPORTID1"))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RewardNotificationDTO.class)
                .isEqualTo(dto);
    }

    @Test
    void testGetExportNotificationsEmpty() {
        Mockito.when(exportDetailServiceMock.getExportNotifications("EXPORTID1", "ORGANIZATIONID1", "INITIATIVEID1", new ExportDetailFilter(), TEST_PAGE_REQUEST))
                .thenReturn(Flux.empty());

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports/{exportId}/content")
                        .build("ORGANIZATIONID1", "INITIATIVEID1", "EXPORTID1"))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RewardNotificationDTO.class)
                .hasSize(0);
    }

    @Test
    void testGetExportNotificationsPagedOk() {
        ExportContentPageDTO page = ExportContentPageDTO.builder()
                .content(List.of(RewardNotificationDTOFaker.mockInstance(1)))
                .pageNo(TEST_PAGE_REQUEST.getPageNumber())
                .pageSize(TEST_PAGE_REQUEST.getPageSize())
                .totalPages(1)
                .totalElements(1)
                .build();

        Mockito.when(exportDetailServiceMock.getExportNotificationsPaged("EXPORTID1", "ORGANIZATIONID1", "INITIATIVEID1",new ExportDetailFilter(), TEST_PAGE_REQUEST))
                .thenReturn(Mono.just(page));

        // used only to let the test run
        ExportContentPageDTO pageEmpty = ExportContentPageDTO.builder()
                .content(Collections.emptyList())
                .pageNo(TEST_PAGE_REQUEST.getPageNumber())
                .pageSize(TEST_PAGE_REQUEST.getPageSize())
                .totalElements(0)
                .totalPages(0)
                .build();
        Mockito.when(exportDetailServiceMock.getExportNotificationEmptyPage(TEST_PAGE_REQUEST)).thenReturn(Mono.just(pageEmpty));
        //

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports/{exportId}/content/paged")
                        .build("ORGANIZATIONID1", "INITIATIVEID1", "EXPORTID1"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ExportContentPageDTO.class)
                .isEqualTo(page);
    }

    @Test
    void testGetExportNotificationsPagedEmpty() {
        ExportContentPageDTO pageEmpty = ExportContentPageDTO.builder()
                .content(Collections.emptyList())
                .pageNo(TEST_PAGE_REQUEST.getPageNumber())
                .pageSize(TEST_PAGE_REQUEST.getPageSize())
                .totalElements(0)
                .totalPages(0)
                .build();

        Mockito.when(exportDetailServiceMock.getExportNotificationsPaged("EXPORTID1", "ORGANIZATIONID1", "INITIATIVEID1", new ExportDetailFilter(), TEST_PAGE_REQUEST))
                .thenReturn(Mono.empty());
        Mockito.when(exportDetailServiceMock.getExportNotificationEmptyPage(TEST_PAGE_REQUEST)).thenReturn(Mono.just(pageEmpty));

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports/{exportId}/content/paged")
                        .build("ORGANIZATIONID1", "INITIATIVEID1", "EXPORTID1"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ExportContentPageDTO.class)
                .isEqualTo(pageEmpty);
    }

    @Test
    void testGetRewardNotificationOk() {
        RewardNotificationDetailDTO dto = RewardNotificationDetailDTOFaker.mockInstance(1);

        Mockito.when(exportDetailServiceMock.getRewardNotification("NOTIFICATIONID1", "ORGANIZATIONID1", "INITIATIVEID1"))
                .thenReturn(Mono.just(dto));

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/byExternalId/{notificationExternalId}")
                        .build("ORGANIZATIONID1", "INITIATIVEID1", "NOTIFICATIONID1"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(RewardNotificationDetailDTO.class)
                .isEqualTo(dto);
    }

    @Test
    void testGetRewardNotificationEmpty() {
        Mockito.when(exportDetailServiceMock.getRewardNotification("NOTIFICATIONID1", "ORGANIZATIONID1", "INITIATIVEID1"))
                .thenReturn(Mono.empty());

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/byExternalId/{notificationExternalId}")
                        .build("ORGANIZATIONID1", "INITIATIVEID1", "NOTIFICATIONID1"))
                .exchange()
                .expectStatus().isNotFound();
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
                .expectBody(new ParameterizedTypeReference<Page<RewardImportsDTO>>() {
                })
                .isEqualTo(pageMock);

        Mockito.verify(organizationImportsServiceMock, Mockito.times(1)).findAllPaged("ORGANIZATION_ID_1", "INITIATIVE_ID_1", TEST_PAGE_REQUEST, new FeedbackImportFilter());
    }

    @Test
    void testGetImportErrorsCsvOk() {
        String expectedCsvString = "";
        String importId = "orgId/initiativeId/import/test.zip";

        Mockito.when(organizationImportsServiceMock.getErrorsCsvByImportId("orgId", "initiativeId", importId))
                .thenReturn(Mono.just(expectedCsvString));

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/imports/{fileName}/errors")
                        .build("orgId", "initiativeId", "test.zip"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("text/csv;charset=UTF-8")
                .expectHeader().contentDisposition(ContentDisposition.attachment().filename("test.zip").build())
                .expectBody(String.class).isEqualTo(expectedCsvString);

        Mockito.verify(organizationImportsServiceMock, Mockito.times(1)).getErrorsCsvByImportId("orgId", "initiativeId", importId);
    }

    @Test
    void testGetImportErrorsEmpty() {
        String importId = "orgId/initiativeId/import/test.zip";

        Mockito.when(organizationImportsServiceMock.getErrorsCsvByImportId("orgId", "initiativeId", importId))
                .thenReturn(Mono.empty());

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/imports/{fileName}/errors")
                        .build("orgId", "initiativeId", "test.zip"))
                .exchange()
                .expectStatus().isNotFound();

        Mockito.verify(organizationImportsServiceMock, Mockito.times(1)).getErrorsCsvByImportId("orgId", "initiativeId", importId);
    }

    @Test
    void testSuspendOk() {
        RewardSuspendedUser expected = new RewardSuspendedUser("userId", "initiativeId", "orgId");
        Mockito.when(userSuspensionServiceMock.suspend("orgId", "initiativeId", "userId"))
                .thenReturn(Mono.just(expected));

        webClient.put()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/user/{userId}/suspend")
                        .build("orgId", "initiativeId", "userId"))
                .exchange()
                .expectStatus().isOk();

        Mockito.verify(userSuspensionServiceMock).suspend("orgId", "initiativeId", "userId");
    }

    @Test
    void testSuspendKo() {
        Mockito.when(userSuspensionServiceMock.suspend("orgId", "initiativeId", "userId"))
                .thenReturn(Mono.empty());

        webClient.put()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/user/{userId}/suspend")
                        .build("orgId", "initiativeId", "userId"))
                .exchange()
                .expectStatus().isNotFound();

        Mockito.verify(userSuspensionServiceMock).suspend("orgId", "initiativeId", "userId");
    }
}