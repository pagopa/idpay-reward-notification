package it.gov.pagopa.reward.notification.service.iban.outcome;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.reward.notification.dto.iban.IbanOutcomeDTO;
import it.gov.pagopa.reward.notification.dto.mapper.IbanOutcomeDTO2RewardIbanMapper;
import it.gov.pagopa.reward.notification.model.RewardIban;
import it.gov.pagopa.reward.notification.service.BaseKafkaConsumer;
import it.gov.pagopa.reward.notification.service.ErrorNotifierService;
import it.gov.pagopa.reward.notification.service.iban.RewardIbanService;
import it.gov.pagopa.reward.notification.service.iban.outcome.filter.IbanOutcomeFilter;
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
public class IbanOutcomeMediatorServiceImpl extends BaseKafkaConsumer<IbanOutcomeDTO, RewardIban> implements IbanOutcomeMediatorService{
    private final Duration commitDelay;

    private final IbanOutcomeDTO2RewardIbanMapper ibanOutcomeDTO2RewardIbanMapper;

    private final IbanOutcomeFilter ibanOutcomeFilter;
    private final RewardIbanService rewardIbanService;
    private final ErrorNotifierService errorNotifierService;

    private final ObjectReader objectReader;

    public IbanOutcomeMediatorServiceImpl(
            @Value("${spring.cloud.stream.kafka.bindings.ibanOutcomeConsumer-in-0.consumer.ackTime}")
            long commitMillis,

            IbanOutcomeDTO2RewardIbanMapper ibanOutcomeDTO2RewardIbanMapper,
            IbanOutcomeFilter ibanOutcomeFilter,

            RewardIbanService rewardIbanService,
            ErrorNotifierService errorNotifierService,

            ObjectMapper objectMapper) {
        this.commitDelay = Duration.ofMillis(commitMillis);
        this.ibanOutcomeDTO2RewardIbanMapper = ibanOutcomeDTO2RewardIbanMapper;
        this.ibanOutcomeFilter = ibanOutcomeFilter;
        this.rewardIbanService = rewardIbanService;
        this.errorNotifierService = errorNotifierService;

        this.objectReader = objectMapper.readerFor(IbanOutcomeDTO.class);
    }

    @Override
    protected Duration getCommitDelay() {
        return commitDelay;
    }

    @Override
    protected ObjectReader getObjectReader() {
        return objectReader;
    }

    @Override
    protected void subscribeAfterCommits(Flux<List<RewardIban>> afterCommits2subscribe) {
        afterCommits2subscribe
                .subscribe(i -> log.debug("[REWARD_NOTIFICATION_IBAN_OUTCOME] Processed offsets for IBAN in outcome topic committed successfully"));
    }

    @Override
    protected Consumer<Throwable> onDeserializationError(Message<String> message) {
        return e -> errorNotifierService.notifyRewardIbanOutcome(message, "[REWARD_NOTIFICATION_IBAN_OUTCOME] Unexpected JSON", false, e);
    }

    @Override
    protected void notifyError(Message<String> message, Throwable e) {
        errorNotifierService.notifyRewardIbanOutcome(message, "[REWARD_NOTIFICATION_IBAN_OUTCOME] An error occurred evaluating iban", false, e);
    }

    @Override
    protected Mono<RewardIban> execute(IbanOutcomeDTO payload, Message<String> message, Map<String, Object> ctx) {
        return Mono.just(payload)
                .filter(ibanOutcomeFilter)
                .map(ibanOutcomeDTO2RewardIbanMapper)
                .flatMap(rewardIbanService::deleteIban);
    }

    @Override
    protected String getFlowName() {
        return "REWARD_NOTIFICATION_IBAN_OUTCOME";
    }

}