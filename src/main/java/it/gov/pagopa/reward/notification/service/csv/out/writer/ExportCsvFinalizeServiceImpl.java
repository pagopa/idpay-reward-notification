package it.gov.pagopa.reward.notification.service.csv.out.writer;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import it.gov.pagopa.reward.notification.connector.azure.storage.RewardsNotificationBlobClient;
import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationExportCsvDto;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationExportStatus;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationExportsRepository;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.email.EmailNotificationService;
import it.gov.pagopa.reward.notification.utils.AuditUtilities;
import it.gov.pagopa.common.utils.ZipUtils;
import it.gov.pagopa.common.utils.csv.HeaderColumnNameStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Service
public class ExportCsvFinalizeServiceImpl implements ExportCsvFinalizeService {

    private final String csvTmpDir;
    private final char csvSeparator;
    private final RewardsNotificationRepository rewardsNotificationRepository;
    private final RewardOrganizationExportsRepository rewardOrganizationExportsRepository;
    private final HeaderColumnNameStrategy<RewardNotificationExportCsvDto> mappingStrategy;
    private final RewardsNotificationBlobClient azureBlobClient;
    private final EmailNotificationService emailNotificationService;
    private final AuditUtilities auditUtilities;
    private final Scheduler rewardNotificationUpdateScheduler;

    public ExportCsvFinalizeServiceImpl(
            @Value("${app.csv.tmp-dir}") String csvTmpDir,
            @Value("${app.csv.export.separator}") char csvSeparator,
            @Value("${app.csv.export.db-update-parallelism}") int parallelism,
            RewardsNotificationRepository rewardsNotificationRepository, RewardOrganizationExportsRepository rewardOrganizationExportsRepository, RewardsNotificationBlobClient azureBlobClient,
            EmailNotificationService emailNotificationService, AuditUtilities auditUtilities) {
        this.csvTmpDir = csvTmpDir;
        this.csvSeparator = csvSeparator;
        this.rewardsNotificationRepository = rewardsNotificationRepository;
        this.rewardOrganizationExportsRepository = rewardOrganizationExportsRepository;
        this.azureBlobClient = azureBlobClient;
        this.emailNotificationService = emailNotificationService;
        this.auditUtilities = auditUtilities;

        mappingStrategy = new HeaderColumnNameStrategy<>(RewardNotificationExportCsvDto.class);
        rewardNotificationUpdateScheduler = Schedulers.newBoundedElastic(parallelism, Integer.MAX_VALUE, "expNotifySave");
    }

    @Override
    public Mono<RewardOrganizationExport> writeCsvAndFinalize(List<RewardNotificationExportCsvDto> csvLines, RewardOrganizationExport export) {
        log.info("[REWARD_NOTIFICATION_EXPORT_CSV] Writing export of initiative {} having id {} on localPath {}/{} containing {} rows", export.getInitiativeId(), export.getId(), csvTmpDir, export.getFilePath(), csvLines.size());

        Path zipFilePath = writeCsv(csvLines, export);

        updateExportCounters(csvLines, export);
        export.setStatus(RewardOrganizationExportStatus.EXPORTED);

        log.info("[REWARD_NOTIFICATION_EXPORT_CSV] Sending to Azure Storage export of initiative {} having id {} on path {}", export.getInitiativeId(), export.getId(), export.getFilePath());
        auditUtilities.logUploadFile(export.getInitiativeId(), export.getOrganizationId(), zipFilePath.toFile().getName());

        return azureBlobClient.uploadFile(zipFilePath.toFile(), export.getFilePath(), "application/zip")
                .filter(r-> {
                    boolean uploadResult = r.getStatusCode() == 201;
                    if(!uploadResult){
                        throw new IllegalStateException("[REWARD_NOTIFICATION_EXPORT_CSV] Something gone wrong while uploading export %s: %s, %s, %s".formatted(export.getFilePath(), r.getStatusCode(), r.getHeaders(), r.getValue()));
                    } else {
                        return true;
                    }
                })
                .doOnNext(x-> {
                    log.info("[REWARD_NOTIFICATION_EXPORT_CSV] Updating exported RewardNotifications statuses {} and relating them to export {}", csvLines.size(), export.getId());
                    try {
                        Files.delete(zipFilePath);
                    } catch (IOException e) {
                        log.warn("[REWARD_NOTIFICATION_EXPORT_CSV] Cannot delete uploaded zip from container {}", zipFilePath, e);
                    }
                })
                .flatMapMany(x -> Flux.fromIterable(csvLines)
                        .publishOn(rewardNotificationUpdateScheduler)
                        .flatMap(l -> rewardsNotificationRepository.updateExportStatus(l.getId(), l.getIban(), l.getCheckIban(), export.getId()))
                        .doOnNext(rId -> log.debug("[REWARD_NOTIFICATION_EXPORT_CSV] Updated exported RewardNotifications status {} and related to export {}", rId, export.getId()))
                )
                .collectList()
                .flatMap(x-> {
                    if(!x.isEmpty()){
                        return rewardOrganizationExportsRepository.save(export);
                    } else {
                        return Mono.empty();
                    }
                })
                .flatMap(emailNotificationService::send)

                .onErrorResume(e -> {
                    log.error("[REWARD_NOTIFICATION_EXPORT_CSV] Something gone wrong while writing export {}", export.getId(), e);
                    export.setStatus(RewardOrganizationExportStatus.ERROR);
                    return rewardOrganizationExportsRepository.save(export)
                            .then(Mono.empty());
                });
    }

    private Path writeCsv(List<RewardNotificationExportCsvDto> csvLines, RewardOrganizationExport export) {
        String localZipFileName = "%s/%s".formatted(csvTmpDir, export.getFilePath());
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

        return Path.of(localZipFileName);
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
