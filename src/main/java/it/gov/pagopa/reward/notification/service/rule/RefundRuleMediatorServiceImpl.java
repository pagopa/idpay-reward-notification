package it.gov.pagopa.reward.notification.service.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.reward.notification.dto.rule.InitiativeRefund2StoreDTO;
import it.gov.pagopa.reward.notification.dto.mapper.Initiative2RewardNotificationRuleMapper;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.service.BaseKafkaConsumer;
import it.gov.pagopa.reward.notification.service.ErrorNotifierService;
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
public class RefundRuleMediatorServiceImpl extends BaseKafkaConsumer<InitiativeRefund2StoreDTO, RewardNotificationRule> implements RefundRuleMediatorService {

    private final Duration commitDelay;

    private final Initiative2RewardNotificationRuleMapper initiative2RewardNotificationRuleMapper;
    private final RewardNotificationRuleValidatorService rewardNotificationRuleValidatorService;
    private final RewardNotificationRuleService rewardNotificationRuleService;
    private final ErrorNotifierService errorNotifierService;

    private final ObjectReader objectReader;

    public RefundRuleMediatorServiceImpl(
            @Value("${spring.cloud.stream.kafka.bindings.refundRuleConsumer-in-0.consumer.ackTime}")
            long commitMillis,
            Initiative2RewardNotificationRuleMapper initiative2RewardNotificationRuleMapper,
            RewardNotificationRuleValidatorService rewardNotificationRuleValidatorService, RewardNotificationRuleService rewardNotificationRuleService,

            ErrorNotifierService errorNotifierService, ObjectMapper objectMapper) {
        this.commitDelay = Duration.ofMillis(commitMillis);
        this.initiative2RewardNotificationRuleMapper = initiative2RewardNotificationRuleMapper;
        this.rewardNotificationRuleValidatorService = rewardNotificationRuleValidatorService;
        this.rewardNotificationRuleService = rewardNotificationRuleService;
        this.errorNotifierService = errorNotifierService;

        this.objectReader = objectMapper.readerFor(InitiativeRefund2StoreDTO.class);
    }


    @Override
    protected Duration getCommitDelay() {
        return commitDelay;
    }

    @Override
    protected void subscribeAfterCommits(Flux<List<RewardNotificationRule>> afterCommits2subscribe) {
        afterCommits2subscribe
                .subscribe(r -> log.debug("[REWARD_NOTIFICATION_RULE] Processed offsets committed successfully"));
    }

    @Override
    protected ObjectReader getObjectReader() {
        return objectReader;
    }

    @Override
    protected Consumer<Throwable> onDeserializationError(Message<String> message) {
        return e -> errorNotifierService.notifyRewardNotifierRule(message, "[REWARD_NOTIFICATION_RULE] Unexpected JSON", true, e);
    }

    @Override
    protected void notifyError(Message<String> message, Throwable e) {
        errorNotifierService.notifyRewardNotifierRule(message, "[REWARD_NOTIFICATION_RULE] An error occurred handling initiative", true, e);
    }

    @Override
    protected Mono<RewardNotificationRule> execute(InitiativeRefund2StoreDTO payload, Message<String> message, Map<String, Object> ctx) {
        return Mono.just(payload)
                .map(initiative2RewardNotificationRuleMapper)
                .doOnNext(rewardNotificationRuleValidatorService::validate)
                .flatMap(rewardNotificationRuleService::save);
    }

    @Override
    protected String getFlowName() {
        return "REWARD_NOTIFICATION_RULE";
    }
}
