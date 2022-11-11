package it.gov.pagopa.reward.notification.exception;

import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.controller.NotificationController;
import it.gov.pagopa.reward.notification.dto.ErrorDTO;
import it.gov.pagopa.reward.notification.dto.controller.ExportFilter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

class ErrorManagerTest extends BaseIntegrationTest {

    @SpyBean
    NotificationController controller;

    @Autowired
    WebTestClient webTestClient;

    @Test
    void handleExceptionClientExceptionNoBody() {
        Mockito.when(controller.getExports("ClientExceptionNoBody", "INITIATIVE_ID", PageRequest.of(0,10), new ExportFilter()))
                .thenThrow(new ClientExceptionNoBody(HttpStatus.NOT_FOUND));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports")
                        .build("ClientExceptionNoBody", "INITIATIVE_ID"))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().isEmpty();
    }

    @Test
    void handleExceptionClientExceptionWithBody(){
        Mockito.when(controller.getExports("ClientExceptionWithBody", "INITIATIVE_ID", PageRequest.of(0,10), new ExportFilter()))
                .thenThrow(new ClientExceptionWithBody(HttpStatus.BAD_REQUEST, "Error","Error ClientExceptionWithBody"));
        ErrorDTO errorClientExceptionWithBody= new ErrorDTO("Error","Error ClientExceptionWithBody");

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports")
                        .build("ClientExceptionWithBody", "INITIATIVE_ID"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorDTO.class).isEqualTo(errorClientExceptionWithBody);

        Mockito.when(controller.getExports("ClientExceptionWithBodyWithStatusAndTitleAndMessageAndThrowable", "INITIATIVE_ID", PageRequest.of(0,10), new ExportFilter()))
                .thenThrow(new ClientExceptionWithBody(HttpStatus.BAD_REQUEST, "Error","Error ClientExceptionWithBody", new Throwable()));
        ErrorDTO errorClientExceptionWithBodyWithStatusAndTitleAndMessageAndThrowable = new ErrorDTO("Error","Error ClientExceptionWithBody");

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports")
                        .build("ClientExceptionWithBodyWithStatusAndTitleAndMessageAndThrowable", "INITIATIVE_ID"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorDTO.class).isEqualTo(errorClientExceptionWithBodyWithStatusAndTitleAndMessageAndThrowable);
    }

    @Test
    void handleExceptionClientExceptionTest(){
        ErrorDTO expectedErrorClientException = new ErrorDTO("Error","Something gone wrong");

        Mockito.when(controller.getExports("ClientException", "INITIATIVE_ID", PageRequest.of(0,10), new ExportFilter()))
                .thenThrow(ClientException.class);
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports")
                        .build("ClientException", "INITIATIVE_ID"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(ErrorDTO.class).isEqualTo(expectedErrorClientException);


        Mockito.when(controller.getExports("ClientExceptionStatusAndMessage", "INITIATIVE_ID", PageRequest.of(0,10), new ExportFilter()))
                .thenThrow(new ClientException(HttpStatus.BAD_REQUEST, "ClientException with httpStatus and message"));
       webTestClient.get()
               .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports")
                       .build("ClientExceptionStatusAndMessage", "INITIATIVE_ID"))
               .exchange()
               .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
               .expectBody(ErrorDTO.class).isEqualTo(expectedErrorClientException);

       Mockito.when(controller.getExports("ClientExceptionStatusAndMessageAndThrowable", "INITIATIVE_ID", PageRequest.of(0,10), new ExportFilter()))
                .thenThrow(new ClientException(HttpStatus.BAD_REQUEST, "ClientException with httpStatus, message and throwable", new Throwable()));
       webTestClient.get()
               .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports")
                       .build("ClientExceptionStatusAndMessageAndThrowable", "INITIATIVE_ID"))
               .exchange()
               .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
               .expectBody(ErrorDTO.class).isEqualTo(expectedErrorClientException);
    }

    @Test
    void handleExceptionRuntimeException(){
        ErrorDTO expectedErrorDefault = new ErrorDTO("Error","Something gone wrong");

        Mockito.when(controller.getExports("RuntimeException", "INITIATIVE_ID", PageRequest.of(0,10), new ExportFilter()))
                .thenThrow(RuntimeException.class);
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/organization/{organizationId}/initiative/{initiativeId}/reward/notification/exports")
                        .build("RuntimeException", "INITIATIVE_ID"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(ErrorDTO.class).isEqualTo(expectedErrorDefault);
    }
}