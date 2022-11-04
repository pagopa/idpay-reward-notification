package it.gov.pagopa.reward.notification.service.csv.export.writer;

import com.opencsv.bean.StatefulBeanToCsv;
import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationExportCsvDto;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import reactor.core.publisher.Mono;

public interface ExportCsvWriterService {
    Mono<RewardsNotification> writeLine(RewardsNotification reward, RewardOrganizationExport export, StatefulBeanToCsv<RewardNotificationExportCsvDto> writer);
}
