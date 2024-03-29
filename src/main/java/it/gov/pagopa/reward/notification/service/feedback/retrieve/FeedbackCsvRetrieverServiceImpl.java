package it.gov.pagopa.reward.notification.service.feedback.retrieve;

import com.opencsv.bean.CsvBindByName;
import it.gov.pagopa.reward.notification.connector.azure.storage.RewardsNotificationBlobClient;
import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationImportCsvDto;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.utils.AuditUtilities;
import it.gov.pagopa.reward.notification.utils.RewardFeedbackConstants;
import it.gov.pagopa.common.utils.ZipUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Slf4j
@Service
public class FeedbackCsvRetrieverServiceImpl implements FeedbackCsvRetrieverService {

    private final String csvTmpDir;
    private final RewardsNotificationBlobClient blobClient;

    private final List<Pattern> mandatoryHeadersPattern;
    private final AuditUtilities auditUtilities;


    public FeedbackCsvRetrieverServiceImpl(
            @Value("${app.csv.tmp-dir}") String csvTmpDir,
            @Value("${app.csv.import.separator}") String csvColumnSeparator,
            RewardsNotificationBlobClient blobClient,
            AuditUtilities auditUtilities) {
        this.csvTmpDir = csvTmpDir;
        this.blobClient = blobClient;
        this.auditUtilities = auditUtilities;

        mandatoryHeadersPattern = Stream.of(
                        RewardNotificationImportCsvDto.Fields.uniqueID,
                        RewardNotificationImportCsvDto.Fields.result)
                .map(f-> Objects.requireNonNull(ReflectionUtils.findField(RewardNotificationImportCsvDto.class, f)))
                .map(f->Objects.requireNonNull(f.getAnnotation(CsvBindByName.class)))
                .map(CsvBindByName::column)
                .map(columnName -> Pattern.compile("(?:^|%s)\"?%s\"?(?:$|%s)".formatted(csvColumnSeparator, columnName, csvColumnSeparator)))
                .toList();
    }

    @Override
    public Mono<Path> retrieveCsv(RewardOrganizationImport importRequest) {
        Path zipLocalPath = Path.of(csvTmpDir, importRequest.getFilePath());
        auditUtilities.logDownloadFile(importRequest.getInitiativeId(), importRequest.getOrganizationId());
        return blobClient.downloadFile(importRequest.getFilePath(), zipLocalPath)
                .mapNotNull(x -> validateZipContentAndUnzip(zipLocalPath, importRequest));
    }

    private Path validateZipContentAndUnzip(Path zipLocalPath, RewardOrganizationImport importRequest) {
        log.info("[REWARD_NOTIFICATION_FEEDBACK] Zip file downloaded, verifying content and unzipping: {}", importRequest.getFilePath());
        try (ZipFile zipFile = new ZipFile(zipLocalPath.toString())) {
            String csvFileName = Path.of(zipFile.getName()).getFileName().toString().replace(".zip", ".csv");

            ZipEntry zipEntry = validateZipContent(zipFile, csvFileName, importRequest);

            if (zipEntry != null) {
                log.info("[REWARD_NOTIFICATION_FEEDBACK] Zip file validated, unzipping it: {}", importRequest.getFilePath());
                Path zipFolder = zipLocalPath.getParent();
                ZipUtils.unzipZipEntry(zipFolder.toString(), zipFile, zipEntry);
                Path csvLocalPath = zipFolder.resolve(csvFileName);
                if(validateCsvHeader(csvLocalPath)){
                    return csvLocalPath;
                } else {
                    addError(importRequest, RewardFeedbackConstants.ImportFileErrors.INVALID_HEADERS);
                    return null;
                }
            } else {
                return null;
            }
        } catch (IOException e) {
            if("zip file is empty".equals(e.getMessage())){
                addError(importRequest, RewardFeedbackConstants.ImportFileErrors.NO_SIZE);
                return null;
            } else {
                throw new IllegalStateException("[REWARD_NOTIFICATION_FEEDBACK] Something gone wrong while handling local zipFile %s".formatted(zipLocalPath), e);
            }
        } finally {
            try {
                Files.deleteIfExists(zipLocalPath);
            } catch (IOException e) {
                log.error("[REWARD_NOTIFICATION_FEEDBACK] Cannot delete local zip file {}", zipLocalPath, e);
            }
        }

    }

    private void addError(RewardOrganizationImport importRequest, RewardFeedbackConstants.ImportFileErrors noSize) {
        importRequest.getErrors().add(new RewardOrganizationImport.RewardOrganizationImportError(noSize));
    }

    private ZipEntry validateZipContent(ZipFile zipFile, String csvFileName, RewardOrganizationImport importRequest) {

        @SuppressWarnings("squid:S5042") // ignoring zip resource consumption alert: we are already checking that there will be just 1 entry, when unzipping it, we are checking also its size
        List<? extends ZipEntry> zipEntries = zipFile
                .stream()
                .filter(f->!f.getName().startsWith("__MACOSX"))
                .toList();

        if(zipEntries.isEmpty()){
            log.info("[REWARD_NOTIFICATION_FEEDBACK] user uploaded an empty zip file: {}", importRequest.getFilePath());
            addError(importRequest, RewardFeedbackConstants.ImportFileErrors.EMPTY_ZIP);
            return null;
        } else if(zipEntries.size() >1) {
            log.info("[REWARD_NOTIFICATION_FEEDBACK] user uploaded a zip having more than one entry: entries:[{}], zipFile={}", zipFile.stream().map(ZipEntry::getName).collect(Collectors.joining(",")), importRequest.getFilePath());
            addError(importRequest, RewardFeedbackConstants.ImportFileErrors.INVALID_CONTENT);
            return null;
        }

        ZipEntry zipEntry = zipEntries.get(0);

        if (!zipEntry.getName().equals(csvFileName)) {
            log.info("[REWARD_NOTIFICATION_FEEDBACK] zipped file has not the expected name: entryName={}, expected={}", zipEntry.getName(), csvFileName);
            addError(importRequest, RewardFeedbackConstants.ImportFileErrors.INVALID_CSV_NAME);
            return null;
        }
        return zipEntry;
    }

    private boolean validateCsvHeader(Path csvLocalPath) {
        try(Stream<String> linesStream = Files.lines(csvLocalPath)) {
            String headerLine = linesStream.limit(1).findFirst().orElse(null);
            if(StringUtils.hasText(headerLine)){
                boolean validHeader = mandatoryHeadersPattern.stream().allMatch(p -> p.matcher(headerLine).find());
                if(!validHeader){
                    log.info("[REWARD_NOTIFICATION_FEEDBACK] csv header is not valid: {}, path: {}", headerLine, csvLocalPath);
                }
                return validHeader;
            } else {
                log.info("[REWARD_NOTIFICATION_FEEDBACK] csv has no lines {}", csvLocalPath);
                return false;
            }
        } catch (IOException e) {
            throw new IllegalStateException("[REWARD_NOTIFICATION_FEEDBACK] Something gone wrong while reading local csvFile %s".formatted(csvLocalPath), e);
        }
    }
}
