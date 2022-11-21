package it.gov.pagopa.reward.notification.service.csv.in;

import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationImportCsvDto;
import it.gov.pagopa.reward.notification.service.csv.in.utils.RewardNotificationFeedbackHandlerOutcome;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class RewardNotificationFeedbackHandlerServiceImpl implements RewardNotificationFeedbackHandlerService {
    @Override
    public Mono<RewardNotificationFeedbackHandlerOutcome> evaluate(RewardNotificationImportCsvDto row) {
        return Mono.empty(); // TODO
    }
}
