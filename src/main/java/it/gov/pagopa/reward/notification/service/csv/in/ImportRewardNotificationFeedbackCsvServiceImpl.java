package it.gov.pagopa.reward.notification.service.csv.in;

import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.nio.file.Path;

@Service
public class ImportRewardNotificationFeedbackCsvServiceImpl implements ImportRewardNotificationFeedbackCsvService {
    @Override
    public Flux<RewardsNotification> evaluate(Path csv, RewardOrganizationImport importRequest) {
        return Flux.empty(); // TODO
    }
}
