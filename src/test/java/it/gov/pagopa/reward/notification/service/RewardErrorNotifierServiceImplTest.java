package it.gov.pagopa.reward.notification.service;

import it.gov.pagopa.common.kafka.service.ErrorNotifierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;

@ExtendWith(MockitoExtension.class)
class RewardErrorNotifierServiceImplTest {

    private RewardErrorNotifierServiceImpl errorNotifierService;
    @Mock
    private ErrorNotifierService mockedErrorNotifierService;
    @Mock
    private Message<?> message;
    @Mock
    private Message<String> message2;

    @BeforeEach
    void setUp() {
        errorNotifierService = new RewardErrorNotifierServiceImpl(mockedErrorNotifierService,
                "refundRuleType", "refundRuleServer", "refundRuleTopic", "refundRuleGroup",
                "rewardResponseType", "rewardResponseServer", "rewardResponseTopic", "rewardResponseGroup",
                "rewardIbanOutcomeType", "rewardIbanOutcomeServer", "rewardIbanOutcomeTopic", "rewardIbanOutcomeGroup",
                "organizationFeedbackUploadType", "organizationFeedbackUploadServer", "organizationFeedbackUploadTopic", "organizationFeedbackUploadGroup",
                "rewardCommandsType", "rewardCommandsServer", "rewardCommandsTopic", "rewardCommandsGroup");
    }

    @Test
    void notifyRewardNotifierRule_ShouldCallErrorNotifierService() {
        String description = "Test description";
        Throwable exception = new RuntimeException("Test exception");

        errorNotifierService.notifyRewardNotifierRule(message, description, true, exception);

        Mockito.verify(mockedErrorNotifierService).notify("refundRuleType", "refundRuleServer", "refundRuleTopic", "refundRuleGroup", message, description, true, true, exception);
    }
    @Test
    void notifyRewardResponse_ShouldCallErrorNotifierService() {
        String description = "Test description";
        Throwable exception = new RuntimeException("Test exception");

        errorNotifierService.notifyRewardResponse(message, description, true, exception);

        Mockito.verify(mockedErrorNotifierService).notify("rewardResponseType", "rewardResponseServer", "rewardResponseTopic", "rewardResponseGroup", message, description, true, true, exception);
    }
    @Test
    void notifyRewardIbanOutcome_ShouldCallErrorNotifierService() {
        String description = "Test description";
        Throwable exception = new RuntimeException("Test exception");

        errorNotifierService.notifyRewardIbanOutcome(message2, description, true, exception);

        Mockito.verify(mockedErrorNotifierService).notify("rewardIbanOutcomeType", "rewardIbanOutcomeServer", "rewardIbanOutcomeTopic", "rewardIbanOutcomeGroup", message2, description, true, true, exception);
    }
    @Test
    void notifyRewardCommands_ShouldCallErrorNotifierService() {
        String description = "Test description";
        Throwable exception = new RuntimeException("Test exception");

        errorNotifierService.notifyRewardCommands(message2, description, true, exception);

        Mockito.verify(mockedErrorNotifierService).notify("rewardCommandsType", "rewardCommandsServer", "rewardCommandsTopic", "rewardCommandsGroup", message2, description, true, true, exception);
    }
    @Test
    void notifyOrganizationFeedbackUpload_ShouldCallErrorNotifierService() {
        String description = "Test description";
        Throwable exception = new RuntimeException("Test exception");

        errorNotifierService.notifyOrganizationFeedbackUpload(message2, description, true, exception);

        Mockito.verify(mockedErrorNotifierService).notify("organizationFeedbackUploadType", "organizationFeedbackUploadServer", "organizationFeedbackUploadTopic", "organizationFeedbackUploadGroup", message2, description, true, true, exception);
    }
}