package it.gov.pagopa.reward.notification.service.exports;

import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationExportsRepository;
import it.gov.pagopa.reward.notification.service.csv.out.ExportRewardNotificationCsvService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
public class ForceOrganizationExportServiceImpl implements ForceOrganizationExportService {

    private final RewardOrganizationExportsRepository rewardOrganizationExportsRepository;
    private final ExportRewardNotificationCsvService exportRewardNotificationCsvService;


    public ForceOrganizationExportServiceImpl(RewardOrganizationExportsRepository rewardOrganizationExportsRepository, ExportRewardNotificationCsvService exportRewardNotificationCsvService) {
        this.rewardOrganizationExportsRepository = rewardOrganizationExportsRepository;
        this.exportRewardNotificationCsvService = exportRewardNotificationCsvService;
    }

    @Override
    public Flux<List<RewardOrganizationExport>> forceExecute(LocalDate notificationDateToSearch) {

        return rewardOrganizationExportsRepository.findByExportDate(LocalDate.now())
                .flatMap(this::cleanTodayExport)
                .thenMany(exportRewardNotificationCsvService.execute(notificationDateToSearch));
    }

    private Mono<RewardOrganizationExport> cleanTodayExport(RewardOrganizationExport export) {
        log.info("[REWARD_ORGANIZATION_EXPORT][FORCED] Setting exportDate of file having id {} to yesterday", export.getId());

        export.setExportDate(export.getExportDate().minusDays(1));
        return rewardOrganizationExportsRepository.save(export);
    }
}
