package it.gov.pagopa.reward.notification.service.feedback.retrieve;

import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.file.Path;

@Service
public class FeedbackCsvRetrieverServiceImpl implements FeedbackCsvRetrieverService{
    @Override
    public Mono<Path> retrieveCsv(RewardOrganizationImport importRequest) {
        return Mono.empty(); // TODO
    }
}
