package it.gov.pagopa.reward.notification.service.csv.in;

import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationImportCsvDto;
import it.gov.pagopa.reward.notification.service.csv.in.utils.RewardNotificationFeedbackHandlerOutcome;
import reactor.core.publisher.Mono;

public interface RewardNotificationFeedbackHandlerService {
    Mono<RewardNotificationFeedbackHandlerOutcome> evaluate(RewardNotificationImportCsvDto row);
}
