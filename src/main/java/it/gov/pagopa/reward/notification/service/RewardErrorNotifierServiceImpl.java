package it.gov.pagopa.reward.notification.service;

import it.gov.pagopa.common.kafka.service.ErrorNotifierService;
import it.gov.pagopa.reward.notification.config.KafkaConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RewardErrorNotifierServiceImpl implements RewardErrorNotifierService {

    private final ErrorNotifierService errorNotifierService;
    private final KafkaConfiguration kafkaConfiguration;

    private static final String REWARD_RULE_CONSUMER_IN_0 = "refundRuleConsumer-in-0";
    private static final  String REWARD_TRX_CONSUMER_IN_0 = "rewardTrxConsumer-in-0";
    private static final  String IBAN_OUTCOME_CONSUMER_IN_0 = "ibanOutcomeConsumer-in-0";
    private static final String REWARD_NOTIFICATION_UPLOAD_CONSUMER_IN_0 = "rewardNotificationUploadConsumer-in-0";
    private static final String COMMANDS_CONSUMER_IN_0 = "commandsConsumer-in-0";

    public RewardErrorNotifierServiceImpl(ErrorNotifierService errorNotifierService,
                                          KafkaConfiguration kafkaConfiguration
    ) {
        this.kafkaConfiguration = kafkaConfiguration;
        this.errorNotifierService = errorNotifierService;
    }
    @Override
    public void notifyRewardNotifierRule(Message<?> message, String description, boolean retryable, Throwable exception) {
        notify(kafkaConfiguration.getStream().getBindings().get(REWARD_RULE_CONSUMER_IN_0), message, description, retryable, true, exception);
    }

    @Override
    public void notifyRewardResponse(Message<?> message, String description, boolean retryable, Throwable exception) {
        notify(kafkaConfiguration.getStream().getBindings().get(REWARD_TRX_CONSUMER_IN_0), message, description, retryable, true, exception);
    }

    @Override
    public void notifyRewardIbanOutcome(Message<String> message, String description, boolean retryable, Throwable exception) {
        notify(kafkaConfiguration.getStream().getBindings().get(IBAN_OUTCOME_CONSUMER_IN_0), message, description, retryable, true, exception);
    }

    @Override
    public void notifyRewardCommands(Message<String> message, String description, boolean retryable, Throwable exception) {
        notify(kafkaConfiguration.getStream().getBindings().get(COMMANDS_CONSUMER_IN_0), message, description, retryable, true, exception);
    }

    @Override
    public void notifyOrganizationFeedbackUpload(Message<String> message, String description, boolean retryable, Throwable exception) {
        notify(kafkaConfiguration.getStream().getBindings().get(REWARD_NOTIFICATION_UPLOAD_CONSUMER_IN_0), message, description, retryable, true, exception);
    }

    @Override
    public void notify(KafkaConfiguration.BaseKafkaInfoDTO baseKafkaInfoDTO, Message<?> message, String description, boolean retryable, boolean resendApplication, Throwable exception) {
        errorNotifierService.notify(baseKafkaInfoDTO, description, retryable,exception, resendApplication, message);
    }
}
