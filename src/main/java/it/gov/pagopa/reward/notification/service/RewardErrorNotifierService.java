package it.gov.pagopa.reward.notification.service;

import it.gov.pagopa.reward.notification.config.KafkaConfiguration;
import org.springframework.messaging.Message;

public interface RewardErrorNotifierService {
    void notifyRewardNotifierRule(Message<?> message, String description, boolean retryable, Throwable exception);
    void notifyRewardResponse(Message<?> message, String description, boolean retryable, Throwable exception);
    void notifyRewardIbanOutcome(Message<String> message, String description, boolean retryable, Throwable exception);
    void notifyRewardCommands(Message<String> message, String description, boolean retryable, Throwable exception);
    void notify(KafkaConfiguration.BaseKafkaInfoDTO baseKafkaInfoDTO, Message<?> message, String description, boolean retryable, boolean resendApplication, Throwable exception);
    void notifyOrganizationFeedbackUpload(Message<String> message, String description, boolean retryable, Throwable exception);
}
