package it.gov.pagopa.reward.notification.service.csv.export.writer;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import it.gov.pagopa.reward.notification.azure.storage.AzureBlobClient;
import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationExportCsvDto;
import it.gov.pagopa.reward.notification.enums.ExportStatus;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationExportsRepository;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.utils.ZipUtils;
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
    private final AzureBlobClient azureBlobClient;

    public ExportCsvFinalizeServiceImpl(
            @Value("${app.csv.export.separator}") char csvSeparator,
            RewardsNotificationRepository rewardsNotificationRepository, RewardOrganizationExportsRepository rewardOrganizationExportsRepository, AzureBlobClient azureBlobClient) {
        this.csvSeparator = csvSeparator;
        this.rewardsNotificationRepository = rewardsNotificationRepository;
        this.rewardOrganizationExportsRepository = rewardOrganizationExportsRepository;
        this.azureBlobClient = azureBlobClient;

        mappingStrategy = new HeaderColumnNameStrategy<>(RewardNotificationExportCsvDto.class);
    }

    @Override
    public Mono<RewardOrganizationExport> writeCsvAndFinalize(List<RewardNotificationExportCsvDto> csvLines, RewardOrganizationExport export) {
        log.info("[REWARD_NOTIFICATION_EXPORT_CSV] Writing export of initiative {} having id {} on localPath /tmp{} containing {} rows", export.getInitiativeId(), export.getId(), export.getFilePath(), csvLines.size());

        String zipFilePath = writeCsv(csvLines, export);

        updateExportCounters(csvLines, export);
        export.setStatus(ExportStatus.EXPORTED);

        log.info("[REWARD_NOTIFICATION_EXPORT_CSV] Sending to Azure Storage export of initiative {} having id {} on path {}", export.getInitiativeId(), export.getId(), export.getFilePath());

        return azureBlobClient.uploadFile(Path.of(zipFilePath).toFile(), export.getFilePath(), "application/zip")
                .doOnNext(x->log.info("[REWARD_NOTIFICATION_EXPORT_CSV] Updating exported RewardNotifications statuses {} and relating them to export {}", csvLines.size(), export.getId()))
                .flatMapMany(x -> Flux.fromIterable(csvLines)
                        .flatMap(l -> rewardsNotificationRepository.updateExportStatus(l.getUniqueID(), l.getIban(), l.getCheckIban(), export.getId()))
                        .doOnNext(rId -> log.debug("[REWARD_NOTIFICATION_EXPORT_CSV] Updated exported RewardNotifications status {} and related to export {}", rId, export.getId()))
                )
                .then(rewardOrganizationExportsRepository.save(export));
    }

    private String writeCsv(List<RewardNotificationExportCsvDto> csvLines, RewardOrganizationExport export) {
        String localZipFileName = "/tmp%s".formatted(export.getFilePath());
        String localCsvFileName = localZipFileName.replaceAll("\\.zip$", ".csv");

        log.debug("[REWARD_NOTIFICATION_EXPORT_CSV] Writing export CSV of initiative {} having id {} on path {}", export.getInitiativeId(), export.getId(), localCsvFileName);

        createDirectoryIfNotExists(localCsvFileName);
        try (FileWriter writer = new FileWriter(localCsvFileName)) {
            StatefulBeanToCsv<RewardNotificationExportCsvDto> csvWriter = buildCsvWriter(writer);
            csvWriter.write(csvLines);
        } catch (IOException | CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
            throw new IllegalStateException("[REWARD_NOTIFICATION_EXPORT_CSV] Cannot create csv writer %s".formatted(localCsvFileName), e);
        }

        try {
            log.debug("[REWARD_NOTIFICATION_EXPORT_CSV] Zipping export of initiative {} having id {} on path {}", export.getInitiativeId(), export.getId(), localZipFileName);
            Path csvFile = Path.of(localCsvFileName);
            ZipUtils.zip(localZipFileName, List.of(csvFile.toFile()));
            Files.delete(csvFile);
        } catch (IOException e) {
            throw new IllegalStateException("[REWARD_NOTIFICATION_EXPORT_CSV] Cannot zip csv file %s".formatted(localCsvFileName), e);
        }

        return localZipFileName;
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
