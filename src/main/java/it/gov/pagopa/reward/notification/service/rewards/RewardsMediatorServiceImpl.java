package it.gov.pagopa.reward.notification.service.rewards;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.model.Rewards;
import it.gov.pagopa.reward.notification.service.BaseKafkaConsumer;
import it.gov.pagopa.reward.notification.service.ErrorNotifierService;
import it.gov.pagopa.reward.notification.service.LockService;
import it.gov.pagopa.reward.notification.service.rewards.evaluate.RewardNotificationRuleEvaluatorService;
import it.gov.pagopa.reward.notification.service.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service
public class RewardsMediatorServiceImpl extends BaseKafkaConsumer<RewardTransactionDTO, List<Rewards>> implements RewardsMediatorService {

    private final LockService lockService;
    private final RewardsService rewardsService;
    private final RewardNotificationRuleEvaluatorService ruleEvaluatorService;
    private final ErrorNotifierService errorNotifierService;

    private final Duration commitDelay;

    private final ObjectReader objectReader;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters constructor alert
    public RewardsMediatorServiceImpl(
            LockService lockService,
            RewardsService rewardsService,
            RewardNotificationRuleEvaluatorService ruleEvaluatorService, ErrorNotifierService errorNotifierService,

            @Value("${spring.cloud.stream.kafka.bindings.rewardTrxConsumer-in-0.consumer.ackTime}") long commitMillis,

            ObjectMapper objectMapper) {
        this.lockService = lockService;
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
        errorNotifierService.notifyRewardResponse(message, "[REWARD_NOTIFICATION] An error occurred evaluating transaction", true, e);
    }

    @Override
    protected Mono<List<Rewards>> execute(RewardTransactionDTO payload, Message<String> message, Map<String, Object> ctx) {
        throw new IllegalStateException("Logic overridden");
    }

    @Override
    protected Mono<List<Rewards>> execute(Message<String> message, Map<String, Object> ctx) {
        return Mono.fromSupplier(() -> {
                    int lockId = -1;
                    String userId = Utils.readUserId(message.getPayload());
                    if (!StringUtils.isEmpty(userId)) {
                        lockId = calculateLockId(userId);
                        lockService.acquireLock(lockId);
                        log.debug("[REWARD_NOTIFICATION] [LOCK_ACQUIRED] trx having userId {} acquired lock having id {}", userId, lockId);
                    }
                    return new MutablePair<>(message, lockId);
                })
                .flatMap(m -> executeAfterLock(m, ctx));
    }

    @Override
    protected ObjectReader getObjectReader() {
        return objectReader;
    }

    @Override
    protected Consumer<Throwable> onDeserializationError(Message<String> message) {
        return e -> errorNotifierService.notifyRewardResponse(message, "[REWARD_NOTIFICATION] Unexpected JSON", true, e);
    }

    private Mono<List<Rewards>> executeAfterLock(Pair<Message<String>, Integer> messageAndLockId, Map<String, Object> ctx) {
        log.trace("[REWARD_NOTIFICATION] Received payload: {}", messageAndLockId.getKey().getPayload());

        final Message<String> message = messageAndLockId.getKey();

        final Consumer<? super Signal<Rewards>> lockReleaser = x -> {
            int lockId = messageAndLockId.getValue();
            if (lockId > -1) {
                lockService.releaseLock(lockId);
                messageAndLockId.setValue(-1);
                log.debug("[REWARD_NOTIFICATION] [LOCK_RELEASED] released lock having id {}", lockId);
            }
        };

        ctx.put(CONTEXT_KEY_START_TIME, System.currentTimeMillis());

        return Mono.just(message)
                .mapNotNull(this::deserializeMessage)
                .flatMapMany(this::readRewards)
                .flatMap(this::checkDuplicateReward)
                .flatMap(r -> retrieveInitiativeAndEvaluate(r, message))
                .doOnEach(lockReleaser)
                .collectList();
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
                .thenReturn(triple);
    }

    public int calculateLockId(String userId) {
        return Math.floorMod(userId.hashCode(), lockService.getBuketSize());
    }


    private Mono<Rewards> retrieveInitiativeAndEvaluate(Triple<RewardTransactionDTO, String, Reward> triple, Message<String> message) {
        String initiativeId = triple.getMiddle();
        Reward reward = triple.getRight();
        RewardTransactionDTO trx = triple.getLeft();

        return ruleEvaluatorService.retrieveAndEvaluate(initiativeId, reward, trx, message);
    }
}
