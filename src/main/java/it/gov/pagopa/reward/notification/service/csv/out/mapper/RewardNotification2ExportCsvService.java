package it.gov.pagopa.reward.notification.service.csv.out.mapper;

import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationExportCsvDto;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import reactor.core.publisher.Mono;

public interface RewardNotification2ExportCsvService {
    Mono<RewardNotificationExportCsvDto> apply(RewardsNotification reward);
}
