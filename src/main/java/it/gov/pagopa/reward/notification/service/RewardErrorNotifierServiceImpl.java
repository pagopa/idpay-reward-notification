package it.gov.pagopa.reward.notification.service;

import it.gov.pagopa.common.kafka.service.ErrorNotifierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RewardErrorNotifierServiceImpl implements RewardErrorNotifierService {

    private final ErrorNotifierService errorNotifierService;

    private final String refundRuleMessagingServiceType;
    private final String refundRuleServer;
    private final String refundRuleTopic;
    private final String refundRuleGroup;

    private final String rewardResponseMessagingServiceType;
    private final String rewardResponseServer;
    private final String rewardResponseTopic;
    private final String rewardResponseGroup;

    private final String rewardIbanOutcomeServiceType;
    private final String rewardIbanOutcomeServer;
    private final String rewardIbanOutcomeTopic;
    private final String rewardIbanOutcomeGroup;

    private final String organizationFeedbackUploadServiceType;
    private final String organizationFeedbackUploadServer;
    private final String organizationFeedbackUploadTopic;
    private final String organizationFeedbackUploadGroup;

    private final String rewardCommandsServiceType;
    private final String rewardCommandsServer;
    private final String rewardCommandsTopic;
    private final String rewardCommandsGroup;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters constructor alert
    public RewardErrorNotifierServiceImpl(ErrorNotifierService errorNotifierService,

                                          @Value("${spring.cloud.stream.binders.kafka-idpay-rule.type}") String refundRuleMessagingServiceType,
                                          @Value("${spring.cloud.stream.binders.kafka-idpay-rule.environment.spring.cloud.stream.kafka.binder.brokers}") String refundRuleServer,
                                          @Value("${spring.cloud.stream.bindings.refundRuleConsumer-in-0.destination}") String refundRuleTopic,
                                          @Value("${spring.cloud.stream.bindings.refundRuleConsumer-in-0.group}") String refundRuleGroup,

                                          @Value("${spring.cloud.stream.binders.kafka-rewarded-transactions.type}") String rewardResponseMessagingServiceType,
                                          @Value("${spring.cloud.stream.binders.kafka-rewarded-transactions.environment.spring.cloud.stream.kafka.binder.brokers}") String rewardResponseServer,
                                          @Value("${spring.cloud.stream.bindings.rewardTrxConsumer-in-0.destination}") String rewardResponseTopic,
                                          @Value("${spring.cloud.stream.bindings.rewardTrxConsumer-in-0.group}") String rewardResponseGroup,

                                          @Value("${spring.cloud.stream.binders.kafka-checkiban-outcome.type}") String rewardIbanOutcomeServiceType,
                                          @Value("${spring.cloud.stream.binders.kafka-checkiban-outcome.environment.spring.cloud.stream.kafka.binder.brokers}") String rewardIbanOutcomeServer,
                                          @Value("${spring.cloud.stream.bindings.ibanOutcomeConsumer-in-0.destination}") String rewardIbanOutcomeTopic,
                                          @Value("${spring.cloud.stream.bindings.ibanOutcomeConsumer-in-0.group}") String rewardIbanOutcomeGroup,

                                          @Value("${spring.cloud.stream.binders.kafka-reward-notification-upload.type}") String organizationFeedbackUploadServiceType,
                                          @Value("${spring.cloud.stream.binders.kafka-reward-notification-upload.environment.spring.cloud.stream.kafka.binder.brokers}") String organizationFeedbackUploadServer,
                                          @Value("${spring.cloud.stream.bindings.rewardNotificationUploadConsumer-in-0.destination}") String organizationFeedbackUploadTopic,
                                          @Value("${spring.cloud.stream.bindings.rewardNotificationUploadConsumer-in-0.group}") String organizationFeedbackUploadGroup,

                                          @Value("${spring.cloud.stream.binders.kafka-commands.type}") String rewardCommandsServiceType,
                                          @Value("${spring.cloud.stream.binders.kafka-commands.environment.spring.cloud.stream.kafka.binder.brokers}") String rewardCommandsServer,
                                          @Value("${spring.cloud.stream.bindings.commandsConsumer-in-0.destination}") String rewardCommandsTopic,
                                          @Value("${spring.cloud.stream.bindings.commandsConsumer-in-0.group}") String rewardCommandsGroup
    ) {
        this.errorNotifierService = errorNotifierService;

        this.refundRuleMessagingServiceType = refundRuleMessagingServiceType;
        this.refundRuleServer = refundRuleServer;
        this.refundRuleTopic = refundRuleTopic;
        this.refundRuleGroup = refundRuleGroup;

        this.rewardResponseMessagingServiceType = rewardResponseMessagingServiceType;
        this.rewardResponseServer = rewardResponseServer;
        this.rewardResponseTopic = rewardResponseTopic;
        this.rewardResponseGroup = rewardResponseGroup;

        this.rewardIbanOutcomeServiceType = rewardIbanOutcomeServiceType;
        this.rewardIbanOutcomeServer = rewardIbanOutcomeServer;
        this.rewardIbanOutcomeTopic = rewardIbanOutcomeTopic;
        this.rewardIbanOutcomeGroup = rewardIbanOutcomeGroup;

        this.organizationFeedbackUploadServiceType = organizationFeedbackUploadServiceType;
        this.organizationFeedbackUploadServer = organizationFeedbackUploadServer;
        this.organizationFeedbackUploadTopic = organizationFeedbackUploadTopic;
        this.organizationFeedbackUploadGroup = organizationFeedbackUploadGroup;

        this.rewardCommandsServiceType = rewardCommandsServiceType;
        this.rewardCommandsServer = rewardCommandsServer;
        this.rewardCommandsTopic = rewardCommandsTopic;
        this.rewardCommandsGroup = rewardCommandsGroup;
    }

    @Override
    public void notifyRewardNotifierRule(Message<?> message, String description, boolean retryable, Throwable exception) {
        notify(refundRuleMessagingServiceType, refundRuleServer, refundRuleTopic, refundRuleGroup, message, description, retryable, true, exception);
    }

    @Override
    public void notifyRewardResponse(Message<?> message, String description, boolean retryable, Throwable exception) {
        notify(rewardResponseMessagingServiceType, rewardResponseServer, rewardResponseTopic, rewardResponseGroup, message, description, retryable, true, exception);
    }

    @Override
    public void notifyRewardIbanOutcome(Message<String> message, String description, boolean retryable, Throwable exception) {
        notify(rewardIbanOutcomeServiceType, rewardIbanOutcomeServer, rewardIbanOutcomeTopic, rewardIbanOutcomeGroup, message, description, retryable, true, exception);
    }

    @Override
    public void notifyRewardCommands(Message<String> message, String description, boolean retryable, Throwable exception) {
        notify(rewardCommandsServiceType, rewardCommandsServer, rewardCommandsTopic, rewardCommandsGroup, message, description, retryable, true, exception);
    }

    @Override
    public void notifyOrganizationFeedbackUpload(Message<String> message, String description, boolean retryable, Throwable exception) {
        notify(organizationFeedbackUploadServiceType, organizationFeedbackUploadServer, organizationFeedbackUploadTopic, organizationFeedbackUploadGroup, message, description, retryable, true, exception);
    }

    @Override
    public void notify(String srcType, String srcServer, String srcTopic, String group, Message<?> message, String description, boolean retryable,boolean resendApplication, Throwable exception) {
        errorNotifierService.notify(srcType, srcServer, srcTopic, group, message, description, retryable,resendApplication, exception);
    }
}
