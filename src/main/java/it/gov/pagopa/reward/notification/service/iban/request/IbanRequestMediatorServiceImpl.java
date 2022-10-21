package it.gov.pagopa.reward.notification.service.iban.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.reward.notification.dto.iban.IbanRequestDTO;
import it.gov.pagopa.reward.notification.dto.mapper.IbanRequestDTO2RewardIbanMapper;
import it.gov.pagopa.reward.notification.model.RewardIban;
import it.gov.pagopa.reward.notification.service.BaseKafkaConsumer;
import it.gov.pagopa.reward.notification.service.ErrorNotifierService;
import it.gov.pagopa.reward.notification.service.iban.RewardIbanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@Slf4j
public class IbanRequestMediatorServiceImpl extends BaseKafkaConsumer<IbanRequestDTO, RewardIban> implements IbanRequestMediatorService {
    private final Duration commitDelay;

    private final IbanRequestDTO2RewardIbanMapper ibanRequestDTO2RewardIbanMapper;
    private final RewardIbanService rewardIbanService;
    private final ErrorNotifierService errorNotifierService;

    private final ObjectReader objectReader;

    public IbanRequestMediatorServiceImpl(
            @Value("${spring.application.name}") String applicationName,
            @Value("${spring.cloud.stream.kafka.bindings.ibanRequestConsumer-in-0.consumer.ackTime}")
            long commitMillis,

            IbanRequestDTO2RewardIbanMapper ibanRequestDTO2RewardIbanMapper,

            RewardIbanService rewardIbanService, ErrorNotifierService errorNotifierService, ObjectMapper objectMapper) {
        super(applicationName);
        this.commitDelay = Duration.ofMillis(commitMillis);
        this.ibanRequestDTO2RewardIbanMapper = ibanRequestDTO2RewardIbanMapper;
        this.rewardIbanService = rewardIbanService;
        this.errorNotifierService = errorNotifierService;

        this.objectReader = objectMapper.readerFor(IbanRequestDTO.class);
    }

    @Override
    protected Duration getCommitDelay() {
        return commitDelay;
    }

    @Override
    protected void subscribeAfterCommits(Flux<List<RewardIban>> afterCommits2subscribe) {
        afterCommits2subscribe
                .subscribe(i -> log.debug("[REWARD_NOTIFICATION_IBAN_REQUEST] Processed offsets for IBAN in request topic committed successfully"));
    }

    @Override
    protected ObjectReader getObjectReader() {
        return objectReader;
    }

    @Override
    protected Consumer<Throwable> onDeserializationError(Message<String> message) {
        return e -> errorNotifierService.notifyRewardIbanRequest(message, "[REWARD_NOTIFICATION_IBAN_REQUEST] Unexpected JSON", false, e);
    }

    @Override
    protected void notifyError(Message<String> message, Throwable e) {
        errorNotifierService.notifyRewardIbanRequest(message, "[REWARD_NOTIFICATION_IBAN_REQUEST] An error occurred evaluating iban", false, e);
    }

    @Override
    protected Mono<RewardIban> execute(IbanRequestDTO payload, Message<String> message, Map<String, Object> ctx) {
        return Mono.just(payload)
                .map(ibanRequestDTO2RewardIbanMapper)
                .flatMap(rewardIbanService::save);
    }

    @Override
    protected String getFlowName() {
        return "REWARD_NOTIFICATION_IBAN_REQUEST";
    }
}