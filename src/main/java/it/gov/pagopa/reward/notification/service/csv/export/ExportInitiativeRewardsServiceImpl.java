package it.gov.pagopa.reward.notification.service.csv.export;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import it.gov.pagopa.reward.notification.dto.rewards.csv.RewardNotificationExportCsvDto;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.csv.export.writer.ExportCsvWriterService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ExportInitiativeRewardsServiceImpl implements ExportInitiativeRewardsService {

    private final char csvSeparator;
    private final int csvMaxRows;

    private final RewardsNotificationRepository rewardsNotificationRepository;
    private final ExportCsvWriterService writerService;

    public ExportInitiativeRewardsServiceImpl(
            @Value("${app.csv.export.separator}") char csvSeparator,
            @Value("${app.csv.export.split-size}") int csvMaxRows,
            RewardsNotificationRepository rewardsNotificationRepository, ExportCsvWriterService writerService) {
        this.csvSeparator = csvSeparator;
        this.csvMaxRows = csvMaxRows;
        this.rewardsNotificationRepository = rewardsNotificationRepository;
        this.writerService = writerService;
    }

    @Override
    public Flux<RewardOrganizationExport> performExport(RewardOrganizationExport export) {
        log.info("[REWARD_NOTIFICATION_EXPORT_CSV] Starting export of reward notification related to initiative {}", export.getInitiativeId());
        long startTime = System.currentTimeMillis();

        Deque<Triple<RewardOrganizationExport, FileWriter, StatefulBeanToCsv<RewardNotificationExportCsvDto>>> splits = new ArrayDeque<>();
        Pair<FileWriter, StatefulBeanToCsv<RewardNotificationExportCsvDto>> csvWriter = buildWriter(export);
        splits.add(Triple.of(export, csvWriter.getKey(), csvWriter.getValue()));

        return rewardsNotificationRepository.findRewards2Notify(export.getInitiativeId())
                .flatMap(r -> {
                    Triple<RewardOrganizationExport, FileWriter, StatefulBeanToCsv<RewardNotificationExportCsvDto>> lastSplit = splits.getLast();
                    return writerService.writeLine(r, lastSplit.getLeft(), lastSplit.getRight());
                })

                .thenMany(Flux.fromIterable(splits).map(Triple::getLeft))
                // TODO finalize close and update status
                .doOnNext(x -> log.info("[PERFORMANCE_LOG][REWARD_NOTIFICATION_EXPORT_CSV] Completed export of reward notification related to initiative: {}ms, initiative {}, fileNames {}", System.currentTimeMillis() - startTime, export.getInitiativeId(), splits.stream().map(Triple::getLeft).map(RewardOrganizationExport::getFilePath).collect(Collectors.joining(","))));
    }

    private Pair<FileWriter, StatefulBeanToCsv<RewardNotificationExportCsvDto>> buildWriter(RewardOrganizationExport export) {
        String localFileName = "/tmp" + export.getFilePath();
        try {
            FileWriter writer = new FileWriter(localFileName);
            return Pair.of(writer, new StatefulBeanToCsvBuilder<RewardNotificationExportCsvDto>(writer)
                    .withSeparator(csvSeparator)
                    .withLineEnd("\n")
                    .build());
        } catch (IOException e) {
            throw new IllegalStateException("[REWARD_NOTIFICATION_EXPORT_CSV] Cannot create csv writer %s".formatted(localFileName), e);
        }
    }



}
