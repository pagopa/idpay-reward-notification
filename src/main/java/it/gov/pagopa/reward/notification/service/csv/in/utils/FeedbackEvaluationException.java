package it.gov.pagopa.reward.notification.service.csv.in.utils;

import it.gov.pagopa.reward.notification.utils.RewardFeedbackConstants;
import lombok.Getter;

@Getter
public class FeedbackEvaluationException extends IllegalArgumentException {
    private final RewardFeedbackConstants.ImportFeedbackRowErrors error;

    public FeedbackEvaluationException(RewardFeedbackConstants.ImportFeedbackRowErrors error) {
        super("Something gone wrong handling feedback: %s".formatted(error));
        this.error = error;
    }
}
