package it.gov.pagopa.reward.notification.service.csv.export.writer;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationExportCsvDto;
import it.gov.pagopa.reward.notification.enums.ExportStatus;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationExportsRepository;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.utils.csv.HeaderColumnNameStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Service
public class ExportCsvFinalizeServiceImpl implements ExportCsvFinalizeService {

    private final char csvSeparator;
    private final RewardsNotificationRepository rewardsNotificationRepository;
    private final RewardOrganizationExportsRepository rewardOrganizationExportsRepository;
    private final HeaderColumnNameStrategy<RewardNotificationExportCsvDto> mappingStrategy;

    public ExportCsvFinalizeServiceImpl(
            @Value("${app.csv.export.separator}") char csvSeparator,
            RewardsNotificationRepository rewardsNotificationRepository, RewardOrganizationExportsRepository rewardOrganizationExportsRepository) {
        this.csvSeparator = csvSeparator;
        this.rewardsNotificationRepository = rewardsNotificationRepository;
        this.rewardOrganizationExportsRepository = rewardOrganizationExportsRepository;

        mappingStrategy = new HeaderColumnNameStrategy<>(RewardNotificationExportCsvDto.class);
    }

    @Override
    public Mono<RewardOrganizationExport> writeCsvAndFinalize(List<RewardNotificationExportCsvDto> csvLines, RewardOrganizationExport export) {
        log.info("[REWARD_NOTIFICATION_EXPORT_CSV] Writing export of initiative {} having id {} on localPath /tmp{} containing {} rows", export.getInitiativeId(), export.getId(), export.getFilePath(), csvLines.size());

        writeCsv(csvLines, export);

        updateExportCounters(csvLines, export);
        export.setStatus(ExportStatus.EXPORTED);

        //TODO compress and send to AzureStorage, then update statuses
//        log.info("[REWARD_NOTIFICATION_EXPORT_CSV] Zipping and sending to Azure Storage export of initiative {} having id {} on path {}", export.getInitiativeId(), export.getId(), export.getFilePath());

        log.info("[REWARD_NOTIFICATION_EXPORT_CSV] Updating exported RewardNotifications statuses {} and relating them to export {}", csvLines.size(), export.getId());
        return Flux.fromIterable(csvLines)
                .flatMap(l -> rewardsNotificationRepository.updateExportStatus(l.getUniqueID(), l.getIban(), l.getCheckIban(), export.getId()))
                .doOnNext(rId -> log.debug("[REWARD_NOTIFICATION_EXPORT_CSV] Updated exported RewardNotifications status {} and related to export {}", rId, export.getId()))
                .then(rewardOrganizationExportsRepository.save(export));
    }

    private void writeCsv(List<RewardNotificationExportCsvDto> csvLines, RewardOrganizationExport export) {
        String localFileName = "/tmp%s".formatted(export.getFilePath().replaceAll("\\.zip$", ".csv"));
        createDirectoryIfNotExists(localFileName);
        try (FileWriter writer = new FileWriter(localFileName)) {
            StatefulBeanToCsv<RewardNotificationExportCsvDto> csvWriter = buildCsvWriter(writer);
            csvWriter.write(csvLines);
        } catch (IOException | CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
            throw new IllegalStateException("[REWARD_NOTIFICATION_EXPORT_CSV] Cannot create csv writer %s".formatted(localFileName), e);
        }
    }

    private static void createDirectoryIfNotExists(String localFileName) {
        Path directory = Paths.get(localFileName).getParent();
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException e) {
                throw new IllegalStateException("[REWARD_NOTIFICATION_EXPORT_CSV] Cannot create directory to store csv %s".formatted(localFileName), e);
            }
        }
    }

    private StatefulBeanToCsv<RewardNotificationExportCsvDto> buildCsvWriter(FileWriter writer) {
        return new StatefulBeanToCsvBuilder<RewardNotificationExportCsvDto>(writer)
                .withMappingStrategy(mappingStrategy)
                .withSeparator(csvSeparator)
                .withLineEnd("\n")
                .build();
    }

    private void updateExportCounters(List<RewardNotificationExportCsvDto> csvLines, RewardOrganizationExport export) {
        export.setRewardNotified((long) csvLines.size());
        export.setRewardsExportedCents(csvLines.stream().mapToLong(RewardNotificationExportCsvDto::getAmount).sum());
    }

}
