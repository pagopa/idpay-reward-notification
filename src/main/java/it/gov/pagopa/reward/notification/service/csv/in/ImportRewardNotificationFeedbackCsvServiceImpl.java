package it.gov.pagopa.reward.notification.service.csv.in;

import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.nio.file.Path;

@Slf4j
@Service
public class ImportRewardNotificationFeedbackCsvServiceImpl implements ImportRewardNotificationFeedbackCsvService {
    @Override
    public Flux<RewardsNotification> evaluate(Path csv, RewardOrganizationImport importRequest) {
        log.info("[REWARD_NOTIFICATION_FEEDBACK] Processing csv file: {}", csv);
        return Flux.empty(); // TODO
    }
}
