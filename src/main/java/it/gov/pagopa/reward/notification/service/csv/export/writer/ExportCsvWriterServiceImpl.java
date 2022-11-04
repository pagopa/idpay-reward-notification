package it.gov.pagopa.reward.notification.service.csv.export.writer;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import it.gov.pagopa.reward.notification.dto.mapper.RewardNotificationExport2CsvMapper;
import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationExportCsvDto;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.service.csv.export.retrieve.Iban2NotifyRetrieverService;
import it.gov.pagopa.reward.notification.service.csv.export.retrieve.User2NotifyRetrieverService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
public class ExportCsvWriterServiceImpl implements ExportCsvWriterService {

    private final Iban2NotifyRetrieverService iban2NotifyRetrieverService;
    private final User2NotifyRetrieverService user2NotifyRetrieverService;
    private final RewardNotificationExport2CsvMapper rewardNotificationExport2CsvMapper;

    public ExportCsvWriterServiceImpl(Iban2NotifyRetrieverService iban2NotifyRetrieverService, User2NotifyRetrieverService user2NotifyRetrieverService, RewardNotificationExport2CsvMapper rewardNotificationExport2CsvMapper) {
        this.iban2NotifyRetrieverService = iban2NotifyRetrieverService;
        this.user2NotifyRetrieverService = user2NotifyRetrieverService;
        this.rewardNotificationExport2CsvMapper = rewardNotificationExport2CsvMapper;
    }

    @Override
    public Mono<RewardsNotification> writeLine(RewardsNotification reward, RewardOrganizationExport export, StatefulBeanToCsv<RewardNotificationExportCsvDto> writer) {
        return iban2NotifyRetrieverService.retrieveIban(reward)
                .flatMap(user2NotifyRetrieverService::retrieveUser)
                .map(r2user -> rewardNotificationExport2CsvMapper.apply(r2user.getKey(), r2user.getValue()))
                .flatMap(csvLine -> writeCsvLine(csvLine, reward, export, writer))
                .doOnNext(r -> {
                    r.setExportId(export.getId());
                    r.setExportDate(LocalDateTime.now());
                    r.setStatus(RewardNotificationStatus.EXPORTED);
                })
                .onErrorResume(e -> {
                    log.error("[REWARD_NOTIFICATION_EXPORT_CSV] Something gone wrong while trying to write reward {} on csv /tmp{}", reward.getId(), export.getFilePath(), e);
                    return Mono.empty();
                });
    }

    private Mono<RewardsNotification> writeCsvLine(RewardNotificationExportCsvDto csvLine, RewardsNotification reward, RewardOrganizationExport export, StatefulBeanToCsv<RewardNotificationExportCsvDto> writer) {
        log.debug("[REWARD_NOTIFICATION_EXPORT_CSV] writing line having id {} on csv /tmp{}", reward.getId(), export.getFilePath());

        try {
            writer.write(csvLine);
            return Mono.just(reward);
        } catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
            log.error("[REWARD_NOTIFICATION_EXPORT_CSV] Cannot write line to file /tmp%s".formatted(export.getFilePath()), e);
            return Mono.empty();
        }
    }
}
