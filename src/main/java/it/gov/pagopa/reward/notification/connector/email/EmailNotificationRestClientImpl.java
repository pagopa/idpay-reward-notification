package it.gov.pagopa.reward.notification.connector.email;

import it.gov.pagopa.reward.notification.dto.email.EmailMessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class EmailNotificationRestClientImpl implements EmailNotificationRestClient {

    private static final String URI = "/notification";
    private final WebClient webClient;

    public EmailNotificationRestClientImpl(@Value("/idpay/email-notification") String emailNotificationUrl,
                                           WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.clone()
                .baseUrl(emailNotificationUrl)
                .build();
    }

    @Override
    public Mono<Void> notify(Mono<EmailMessageDTO> emailMessageMono) {
        log.info("[REWARD_NOTIFICATION][EMAIL] Sending email");
        return webClient.method(HttpMethod.POST)
                .uri(URI)
                .body(emailMessageMono, EmailMessageDTO.class)
                .retrieve()
                .bodyToMono(Void.class)

                .onErrorResume(WebClientResponseException.NotFound.class, x -> {
                    log.warn("[REWARD_NOTIFICATION][EMAIL] Something went wrong: NOT_FOUND", x);
                    return Mono.empty();
                })
                .onErrorResume(WebClientResponseException.BadRequest.class, x -> {
                    log.warn("[REWARD_NOTIFICATION][EMAIL] Something went wrong: BAD_REQUEST", x);
                    return Mono.empty();
                });
    }
}
