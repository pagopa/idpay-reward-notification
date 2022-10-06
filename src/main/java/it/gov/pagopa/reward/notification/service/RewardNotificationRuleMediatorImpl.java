package it.gov.pagopa.reward.notification.service;

import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.reward.notification.dto.InitiativeDTO;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class RewardNotificationRuleMediatorImpl extends BaseKafkaConsumer<InitiativeDTO, RewardNotificationRule> implements RewardNotificationRuleMediator{

    @Override
    protected Duration getCommitDelay() {
        return null;
    }

    @Override
    protected void subscribeAfterCommits(Flux<List<RewardNotificationRule>> afterCommits2subscribe) {

    }

    @Override
    protected ObjectReader getObjectReader() {
        return null;
    }

    @Override
    protected Consumer<Throwable> onDeserializationError(Message<String> message) {
        return null;
    }

    @Override
    protected void notifyError(Message<String> message, Throwable e) {

    }

    @Override
    protected Mono<RewardNotificationRule> execute(InitiativeDTO payload, Message<String> message, Map<String, Object> ctx) {
        return null;
    }
}
