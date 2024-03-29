package it.gov.pagopa.reward.notification.service.csv;

import it.gov.pagopa.reward.notification.dto.mapper.RewardFeedbackMapper;
import it.gov.pagopa.reward.notification.dto.rewards.RewardFeedbackDTO;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

@Slf4j
@Service
public class RewardNotificationNotifierServiceImpl implements RewardNotificationNotifierService {

    private final RewardFeedbackMapper mapper;
    private final StreamBridge streamBridge;

    public RewardNotificationNotifierServiceImpl(RewardFeedbackMapper mapper, StreamBridge streamBridge) {
        this.mapper = mapper;
        this.streamBridge = streamBridge;
    }

    /** Declared just to let know Spring to connect the producer at startup */
    @Configuration
    static class RewardNotificationNotifierProducerConfig {
        @Bean
        public Supplier<Flux<Message<RewardFeedbackDTO>>> rewardNotificationFeedback() {
            return Flux::empty;
        }
    }

    @Override
    public Mono<RewardsNotification> notify(RewardsNotification notification, long deltaRewardCents) {
        log.debug("[REWARD_NOTIFICATION_FEEDBACK] Notifying reward feedback {} of export {} with status {} and deltaRewardCents {}", notification.getId(), notification.getExportId(), notification.getStatus(), deltaRewardCents);

        Message<RewardFeedbackDTO> message = MessageBuilder.withPayload(mapper.apply(notification, deltaRewardCents))
                .setHeader(KafkaHeaders.KEY, "%s_%s".formatted(notification.getBeneficiaryId(), notification.getInitiativeId()))
                .build();

        if (!streamBridge.send("rewardNotificationFeedback-out-0", message)) {
            log.error("[REWARD_NOTIFICATION_FEEDBACK] Something gone wrong while notifying reward notification feedback {}", message.getPayload());
        }

        return Mono.just(notification);
    }
}
