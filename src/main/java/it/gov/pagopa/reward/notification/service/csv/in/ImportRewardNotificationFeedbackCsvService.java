package it.gov.pagopa.reward.notification.service.csv.in;

import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import reactor.core.publisher.Mono;

import java.nio.file.Path;

public interface ImportRewardNotificationFeedbackCsvService {
    Mono<RewardOrganizationImport> evaluate(Path csv, RewardOrganizationImport importRequest);
}
