package it.gov.pagopa.reward.notification.service.csv;

import it.gov.pagopa.reward.notification.dto.mapper.RewardFeedbackMapper;
import it.gov.pagopa.reward.notification.dto.rewards.RewardFeedbackDTO;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class RewardNotificationNotifierServiceImpl implements RewardNotificationNotifierService {

    private final RewardFeedbackMapper mapper;
    private final StreamBridge streamBridge;

    public RewardNotificationNotifierServiceImpl(RewardFeedbackMapper mapper, StreamBridge streamBridge) {
        this.mapper = mapper;
        this.streamBridge = streamBridge;
    }

    @Override
    public Mono<RewardsNotification> notify(RewardsNotification notification, long deltaRewardCents) {
        log.debug("[REWARD_NOTIFICATION_FEEDBACK] Notifying reward feedback {} of export {} with status {} and deltaRewardCents {}", notification.getId(), notification.getExportId(), notification.getStatus(), deltaRewardCents);

        Message<RewardFeedbackDTO> message = MessageBuilder.withPayload(mapper.apply(notification, deltaRewardCents))
                .setHeader(KafkaHeaders.MESSAGE_KEY, "%s_%s".formatted(notification.getUserId(), notification.getInitiativeId()))
                .build();

        if (!streamBridge.send("rewardNotificationFeedback-out-0", message)) {
            log.error("[REWARD_NOTIFICATION_FEEDBACK] Something gone wrong while notifying reward notification feedback {}", message.getPayload());
        }

        return Mono.just(notification);
    }
}
