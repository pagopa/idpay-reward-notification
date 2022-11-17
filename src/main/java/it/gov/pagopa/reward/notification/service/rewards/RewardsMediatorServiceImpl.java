package it.gov.pagopa.reward.notification.service.rewards;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.model.Rewards;
import it.gov.pagopa.reward.notification.service.BaseKafkaBlockingPartitionConsumer;
import it.gov.pagopa.reward.notification.service.ErrorNotifierService;
import it.gov.pagopa.reward.notification.service.LockService;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.RewardNotificationRuleEvaluatorService;
import it.gov.pagopa.reward.notification.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service
public class RewardsMediatorServiceImpl extends BaseKafkaBlockingPartitionConsumer<RewardTransactionDTO, List<Rewards>> implements RewardsMediatorService {

    private final RewardsService rewardsService;
    private final RewardNotificationRuleEvaluatorService ruleEvaluatorService;
    private final ErrorNotifierService errorNotifierService;

    private final Duration commitDelay;

    private final ObjectReader objectReader;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters constructor alert
    public RewardsMediatorServiceImpl(
            @Value("${spring.application.name}") String applicationName,
            LockService lockService,
            RewardsService rewardsService,
            RewardNotificationRuleEvaluatorService ruleEvaluatorService, ErrorNotifierService errorNotifierService,

            @Value("${spring.cloud.stream.kafka.bindings.rewardTrxConsumer-in-0.consumer.ackTime}") long commitMillis,

            ObjectMapper objectMapper) {
        super(applicationName, lockService);

        this.rewardsService = rewardsService;
        this.ruleEvaluatorService = ruleEvaluatorService;
        this.errorNotifierService = errorNotifierService;
        this.commitDelay = Duration.ofMillis(commitMillis);

        this.objectReader = objectMapper.readerFor(RewardTransactionDTO.class);
    }

    @Override
    protected Duration getCommitDelay() {
        return commitDelay;
    }

    @Override
    protected void subscribeAfterCommits(Flux<List<List<Rewards>>> afterCommits2subscribe) {
        afterCommits2subscribe.subscribe(p -> log.debug("[REWARD_NOTIFICATION] Processed offsets committed successfully"));
    }

    @Override
    protected void notifyError(Message<String> message, Throwable e) {
        errorNotifierService.notifyRewardResponse(message, "[REWARD_NOTIFICATION] An error occurred evaluating transaction result", true, e);
    }

    @Override
    protected ObjectReader getObjectReader() {
        return objectReader;
    }

    @Override
    protected Consumer<Throwable> onDeserializationError(Message<String> message) {
        return e -> errorNotifierService.notifyRewardResponse(message, "[REWARD_NOTIFICATION] Unexpected JSON", true, e);
    }

    @Override
    protected Mono<List<Rewards>> execute(RewardTransactionDTO payload, Message<String> message, Map<String, Object> ctx) {
        return Mono.just(payload)
                .flatMapMany(this::readRewards)
                .flatMap(this::checkDuplicateReward)
                .flatMap(r -> retrieveInitiativeAndEvaluate(r, message))
                .collectList();
    }

    @Override
    protected int getMessagePartitionKey(Message<String> message) {
        String userId=Utils.readUserId(message.getPayload());
        if(!StringUtils.isEmpty(userId)){
            return userId.hashCode();
        } else {
            return super.getMessagePartitionKey(message);
        }
    }

    @Override
    protected String getFlowName() {
        return "REWARD_NOTIFICATION";
    }

    private Flux<Triple<RewardTransactionDTO, String, Reward>> readRewards(RewardTransactionDTO rewardTransactionDTO) {
        if (CollectionUtils.isEmpty(rewardTransactionDTO.getRewards())) {
            return Flux.empty();
        } else {
            return Flux.fromStream(
                    rewardTransactionDTO.getRewards().entrySet().stream()
                            .map(e -> Triple.of(rewardTransactionDTO, e.getKey(), e.getValue())));
        }
    }

    private Mono<Triple<RewardTransactionDTO, String, Reward>> checkDuplicateReward(Triple<RewardTransactionDTO, String, Reward> triple) {
        return rewardsService.checkDuplicateReward(triple.getLeft(), triple.getMiddle())
                .map(x -> triple);
    }

    private Mono<Rewards> retrieveInitiativeAndEvaluate(Triple<RewardTransactionDTO, String, Reward> triple, Message<String> message) {
        String initiativeId = triple.getMiddle();
        Reward reward = triple.getRight();
        RewardTransactionDTO trx = triple.getLeft();

        return ruleEvaluatorService.retrieveAndEvaluate(initiativeId, reward, trx, message);
    }
}
