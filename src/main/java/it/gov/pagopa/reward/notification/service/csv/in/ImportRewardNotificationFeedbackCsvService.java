package it.gov.pagopa.reward.notification.service.csv.in;

import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import reactor.core.publisher.Flux;

import java.nio.file.Path;

public interface ImportRewardNotificationFeedbackCsvService {
    Flux<RewardsNotification> evaluate(Path csv, RewardOrganizationImport importRequest);
}
