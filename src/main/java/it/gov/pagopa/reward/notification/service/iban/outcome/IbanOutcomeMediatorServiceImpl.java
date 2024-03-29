package it.gov.pagopa.reward.notification.service.iban.outcome;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.reward.notification.dto.iban.IbanOutcomeDTO;
import it.gov.pagopa.reward.notification.model.RewardIban;
import it.gov.pagopa.common.reactive.kafka.consumer.BaseKafkaBlockingPartitionConsumer;
import it.gov.pagopa.reward.notification.service.RewardErrorNotifierService;
import it.gov.pagopa.common.reactive.service.LockService;
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
public class IbanOutcomeMediatorServiceImpl extends BaseKafkaBlockingPartitionConsumer<IbanOutcomeDTO, RewardIban> implements IbanOutcomeMediatorService{
    private final Duration commitDelay;

    private final IbanOutcomeOperationsService ibanOutcomeOperationsService;
    private final RewardErrorNotifierService rewardErrorNotifierService;

    private final ObjectReader objectReader;

    public IbanOutcomeMediatorServiceImpl(
            @Value("${spring.application.name}") String applicationName,
            @Value("${spring.cloud.stream.kafka.bindings.ibanOutcomeConsumer-in-0.consumer.ackTime}")
            long commitMillis,
            IbanOutcomeOperationsService ibanOutcomeOperationsService, RewardErrorNotifierService rewardErrorNotifierService,
            LockService lockService,
            ObjectMapper objectMapper) {
        super(applicationName, lockService);
        this.commitDelay = Duration.ofMillis(commitMillis);
        this.ibanOutcomeOperationsService = ibanOutcomeOperationsService;

        this.rewardErrorNotifierService = rewardErrorNotifierService;

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
                .subscribe(i -> log.info("[REWARD_NOTIFICATION_IBAN_OUTCOME] Processed offsets for IBAN in outcome topic committed successfully"));
    }

    @Override
    protected Consumer<Throwable> onDeserializationError(Message<String> message) {
        return e -> rewardErrorNotifierService.notifyRewardIbanOutcome(message, "[REWARD_NOTIFICATION_IBAN_OUTCOME] Unexpected JSON", false, e);
    }

    @Override
    protected void notifyError(Message<String> message, Throwable e) {
        rewardErrorNotifierService.notifyRewardIbanOutcome(message, "[REWARD_NOTIFICATION_IBAN_OUTCOME] An error occurred evaluating iban", false, e);
    }

    @Override
    protected Mono<RewardIban> execute(IbanOutcomeDTO payload, Message<String> message, Map<String, Object> ctx) {
        return Mono.just(payload)
                .flatMap(ibanOutcomeOperationsService::execute);
    }

    @Override
    public String getFlowName() {
        return "REWARD_NOTIFICATION_IBAN_OUTCOME";
    }

}