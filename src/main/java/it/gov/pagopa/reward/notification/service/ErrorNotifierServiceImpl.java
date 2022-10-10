package it.gov.pagopa.reward.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class ErrorNotifierServiceImpl implements ErrorNotifierService{
    public static final String ERROR_MSG_HEADER_SRC_TYPE = "srcType";
    public static final String ERROR_MSG_HEADER_SRC_SERVER = "srcServer";
    public static final String ERROR_MSG_HEADER_SRC_TOPIC = "srcTopic";
    public static final String ERROR_MSG_HEADER_DESCRIPTION = "description";
    public static final String ERROR_MSG_HEADER_RETRYABLE = "retryable";
    public static final String ERROR_MSG_HEADER_STACKTRACE = "stacktrace";

    private final StreamBridge streamBridge;

    private final String refundRuleMessagingServiceType;
    private final String refundRuleServer;
    private final String refundRuleTopic;

    private final String rewardResponseMessagingServiceType;
    private final String rewardResponseServer;
    private final String rewardResponseTopic;

    public ErrorNotifierServiceImpl(StreamBridge streamBridge,

                                    @Value("${spring.cloud.stream.binders.kafka-idpay-rule.type}") String refundRuleMessagingServiceType,
                                    @Value("${spring.cloud.stream.binders.kafka-idpay-rule.environment.spring.cloud.stream.kafka.binder.brokers}") String refundRuleServer,
                                    @Value("${spring.cloud.stream.bindings.refundRuleConsumer-in-0.destination}") String refundRuleTopic,

                                    @Value("${spring.cloud.stream.binders.kafka-rewarded-transactions.type}") String rewardResponseMessagingServiceType,
                                    @Value("${spring.cloud.stream.binders.kafka-rewarded-transactions.environment.spring.cloud.stream.kafka.binder.brokers}") String rewardResponseServer,
                                    @Value("${spring.cloud.stream.bindings.rewardTrxConsumer-in-0.destination}") String rewardResponseTopic) {
        this.streamBridge = streamBridge;

        this.refundRuleMessagingServiceType = refundRuleMessagingServiceType;
        this.refundRuleServer = refundRuleServer;
        this.refundRuleTopic = refundRuleTopic;

        this.rewardResponseMessagingServiceType = rewardResponseMessagingServiceType;
        this.rewardResponseServer = rewardResponseServer;
        this.rewardResponseTopic = rewardResponseTopic;
    }

    @Override
    public void notifyRewardNotifierRule(Message<?> message, String description, boolean retryable, Throwable exception) {
        notify(refundRuleMessagingServiceType, refundRuleServer, refundRuleTopic, message, description, retryable, exception);
    }

    @Override
    public void notifyRewardResponse(Message<?> message, String description, boolean retryable, Throwable exception) {
        notify(rewardResponseMessagingServiceType, rewardResponseServer, rewardResponseTopic, message, description, retryable, exception);
    }

    @Override
    public void notify(String srcType, String srcServer, String srcTopic, Message<?> message, String description, boolean retryable, Throwable exception) {
        log.info("[ERROR_NOTIFIER] notifying error: {}", description, exception);
        final MessageBuilder<?> errorMessage = MessageBuilder.fromMessage(message)
                .setHeader(ERROR_MSG_HEADER_SRC_TYPE, srcType)
                .setHeader(ERROR_MSG_HEADER_SRC_SERVER, srcServer)
                .setHeader(ERROR_MSG_HEADER_SRC_TOPIC, srcTopic)
                .setHeader(ERROR_MSG_HEADER_DESCRIPTION, description)
                .setHeader(ERROR_MSG_HEADER_RETRYABLE, retryable)
                .setHeader(ERROR_MSG_HEADER_STACKTRACE, ExceptionUtils.getStackTrace(exception));

        addExceptionInfo(errorMessage, "rootCause", ExceptionUtils.getRootCause(exception));
        addExceptionInfo(errorMessage, "cause", exception.getCause());

        byte[] receivedKey = message.getHeaders().get(KafkaHeaders.RECEIVED_MESSAGE_KEY, byte[].class);
        if(receivedKey!=null){
            errorMessage.setHeader(KafkaHeaders.MESSAGE_KEY, new String(receivedKey, StandardCharsets.UTF_8));
        }

        if (!streamBridge.send("errors-out-0", errorMessage.build())) {
            log.error("[ERROR_NOTIFIER] Something gone wrong while notifying error");
        }
    }

    private void addExceptionInfo(MessageBuilder<?> errorMessage, String exceptionHeaderPrefix, Throwable rootCause) {
        errorMessage
                .setHeader("%sClass".formatted(exceptionHeaderPrefix), rootCause != null ? rootCause.getClass().getName() : null)
                .setHeader("%sMessage".formatted(exceptionHeaderPrefix), rootCause != null ? rootCause.getMessage() : null);
    }
}
