package it.gov.pagopa.reward.notification.service;

import org.springframework.messaging.Message;

public interface RewardErrorNotifierService {
    void notifyRewardNotifierRule(Message<?> message, String description, boolean retryable, Throwable exception);
    void notifyRewardResponse(Message<?> message, String description, boolean retryable, Throwable exception);
    void notifyRewardIbanOutcome(Message<String> message, String description, boolean retryable, Throwable exception);
    void notifyRewardCommands(Message<String> message, String description, boolean retryable, Throwable exception);
    @SuppressWarnings("squid:S00107") // suppressing too many parameters alert
    void notify(String srcType, String srcServer, String srcTopic, String group, Message<?> message, String description, boolean retryable,boolean resendApplication,  Throwable exception);

    void notifyOrganizationFeedbackUpload(Message<String> message, String description, boolean retryable, Throwable exception);
}
