package it.gov.pagopa.reward.notification.service.feedback.retrieve;

import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import reactor.core.publisher.Mono;

import java.nio.file.Path;

public interface FeedbackCsvRetrieverService {
    Mono<Path> retrieveCsv(RewardOrganizationImport importRequest);
}
