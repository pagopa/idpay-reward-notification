package it.gov.pagopa.reward.notification.service.csv.out.retrieve;

import com.mongodb.DuplicateKeyException;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationExportStatus;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardNotificationRuleRepository;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationExportsRepository;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.utils.ExportConstants;
import it.gov.pagopa.reward.notification.utils.ExportCsvConstants;
import it.gov.pagopa.reward.notification.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class Initiative2ExportRetrieverServiceImpl implements Initiative2ExportRetrieverService {

    private final String exportBasePath;

    private final RewardOrganizationExportsRepository rewardOrganizationExportsRepository;
    private final RewardsNotificationRepository rewardsNotificationRepository;
    private final RewardNotificationRuleRepository rewardNotificationRuleRepository;


    public Initiative2ExportRetrieverServiceImpl(
            @Value("${app.csv.storage.base-path}") String exportBasePath,
            RewardOrganizationExportsRepository rewardOrganizationExportsRepository, RewardsNotificationRepository rewardsNotificationRepository, RewardNotificationRuleRepository rewardNotificationRuleRepository) {
        this.rewardOrganizationExportsRepository = rewardOrganizationExportsRepository;
        this.rewardsNotificationRepository = rewardsNotificationRepository;
        this.rewardNotificationRuleRepository = rewardNotificationRuleRepository;

        if (StringUtils.isEmpty(exportBasePath)) {
            this.exportBasePath = "";
        } else {
            if (exportBasePath.endsWith("/")) {
                this.exportBasePath = exportBasePath;
            } else {
                this.exportBasePath = "%s/".formatted(exportBasePath);
            }
        }
    }

    @Override
    public Mono<RewardOrganizationExport> retrieveStuckExecution() {
        return rewardOrganizationExportsRepository.reserveStuckExport()
                .doOnNext(reservation -> log.info("[REWARD_NOTIFICATION_EXPORT_CSV] reserved stuck export on initiative into file: {} {} {}", reservation.getId(), reservation.getInitiativeId(), reservation.getFilePath()));
    }

    @Override
    public Mono<RewardOrganizationExport> retrieve(LocalDate notificationDateToSearch) {
        return rewardOrganizationExportsRepository.reserveExport()
                .switchIfEmpty(retrieveNewExports(notificationDateToSearch))
                .doOnNext(reservation -> log.info("[REWARD_NOTIFICATION_EXPORT_CSV] reserved export on initiative into file: {} {} {}", reservation.getId(), reservation.getInitiativeId(), reservation.getFilePath()));
    }

    private Mono<RewardOrganizationExport> retrieveNewExports(LocalDate notificationDateToSearch) {
        log.info("[REWARD_NOTIFICATION_EXPORT_CSV] searching for rewards to notify");

        return rewardOrganizationExportsRepository.findPendingOrTodayExports()

                // For functional tests purposes, clean the exportDate of one exported today
                .flatMap(x -> cleanTodayExport(x, notificationDateToSearch))

                .map(RewardOrganizationExport::getInitiativeId)
                .collect(Collectors.toSet())
                .transformDeferredContextual((ids, ctx) -> ids.map(initiativeIds -> {
                    Set<String> initiativeIds2exclude = new HashSet<>(initiativeIds);
                    initiativeIds2exclude.addAll(ctx.<Set<String>>getOrEmpty(ExportCsvConstants.CTX_KEY_EXPORTED_INITIATIVE_IDS).orElse(Collections.emptySet()));
                    return initiativeIds2exclude;
                }))
                .doOnNext(excludes -> log.info("[REWARD_NOTIFICATION_EXPORT_CSV] excluding exports on initiatives because pending or performed today: {}", excludes))

                // For functional tests purposes, clean the notifications exported today
                .flatMap(excludes -> rewardsNotificationRepository.findNotificationsToReset(excludes, notificationDateToSearch)
                        .flatMap(n -> cleanAlreadyExportedNotifications(n, notificationDateToSearch))
                        .then(Mono.just(excludes)))

                .flatMapMany(excludes -> rewardsNotificationRepository.findInitiatives2Notify(excludes, notificationDateToSearch))
                .flatMap(this::configureExport)
                .collectList()
                .doOnNext(newExports -> log.info("[REWARD_NOTIFICATION_EXPORT_CSV] new exports configured on initiatives: {}", newExports.stream().map(RewardOrganizationExport::getInitiativeId).toList()))
                .flatMap(x -> rewardOrganizationExportsRepository.reserveExport());
    }

    private Mono<RewardOrganizationExport> configureExport(String initiativeId) {
        log.debug("[REWARD_NOTIFICATION_EXPORT_CSV] trying to configure export on initiative: {}", initiativeId);
        LocalDate now = LocalDate.now();

        return rewardNotificationRuleRepository.findById(initiativeId)
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("[REWARD_NOTIFICATION_EXPORT_CSV] configured a reward to notify on not existent initiative {}", initiativeId);
                    return Mono.empty();
                }))
                .flatMap(rule -> rewardOrganizationExportsRepository.count(Example.of(RewardOrganizationExport.builder()
                                .initiativeId(initiativeId)
                                .notificationDate(now)
                                .build()))
                        .defaultIfEmpty(0L)
                        .map(progressive -> buildNewRewardOrganizationExportEntity(rule, now, progressive + 1)))
                .flatMap(e ->
                        rewardOrganizationExportsRepository.configureNewExport(e)
                                .onErrorResume(DuplicateKeyException.class, ex -> Mono.empty())
                );
    }

    public RewardOrganizationExport buildNewRewardOrganizationExportEntity(RewardNotificationRule rule, LocalDate now, long progressive) {
        log.debug("[REWARD_NOTIFICATION_EXPORT_CSV] trying to configure export on existing initiative: {}", rule.getInitiativeId());

        String nowFormatted = Utils.FORMATTER_DATE.format(now);
        return RewardOrganizationExport.builder()
                .id("%s_%s.%d".formatted(rule.getInitiativeId(), nowFormatted, progressive))
                .filePath("%s%s/%s/export/%s".formatted(
                        exportBasePath,
                        rule.getOrganizationId(),
                        rule.getInitiativeId(),
                        buildExportFileName(rule, nowFormatted, progressive)
                ))
                .initiativeId(rule.getInitiativeId())
                .initiativeName(rule.getInitiativeName())
                .organizationId(rule.getOrganizationId())
                .notificationDate(now)
                .progressive(progressive)
                .status(RewardOrganizationExportStatus.TO_DO)

                .rewardsExportedCents(0L)
                .rewardsResultsCents(0L)

                .rewardNotified(0L)
                .rewardsResulted(0L)
                .rewardsResultedOk(0L)

                .percentageResults(0L)
                .percentageResulted(0L)
                .percentageResultedOk(0L)

                .build();
    }

    private String buildExportFileName(RewardNotificationRule rule, String dateFormatted, long progressive) {
        return "%s_%s.%d.zip".formatted(
                escapeRuleName(rule.getInitiativeName()),
                dateFormatted, progressive);
    }

    private String escapeRuleName(String initiativeName) {
        return StringUtils.left(initiativeName.replaceAll("\\W", ""), 10);
    }

    @Override
    public Mono<RewardOrganizationExport> reserveNextSplitExport(RewardOrganizationExport baseExport, int splitNumber) {
        return rewardOrganizationExportsRepository.save(buildNextOrganizationExportSplit(baseExport, splitNumber));
    }

    public RewardOrganizationExport buildNextOrganizationExportSplit(RewardOrganizationExport baseExport, int splitNumber) {
        log.debug("[REWARD_NOTIFICATION_EXPORT_CSV] trying to configure export on next split based on {} of inititiative {} and splitNumber {}", baseExport.getId(), baseExport.getInitiativeId(), splitNumber);

        long progressive = baseExport.getProgressive() + splitNumber;
        return baseExport.toBuilder()
                .id(baseExport.getId().replaceFirst("\\.%d$".formatted(baseExport.getProgressive()), ".%d".formatted(progressive)))
                .filePath(baseExport.getFilePath().replaceFirst("\\.%d.zip$".formatted(baseExport.getProgressive()), ".%d.zip".formatted(progressive)))
                .progressive(progressive)
                .status(RewardOrganizationExportStatus.IN_PROGRESS)

                .rewardsExportedCents(0L)
                .rewardsResultsCents(0L)

                .rewardNotified(0L)
                .rewardsResulted(0L)
                .rewardsResultedOk(0L)

                .percentageResults(0L)
                .percentageResulted(0L)
                .percentageResultedOk(0L)

                .build();
    }

    //region functional tests
    /**
     * For functional tests purposes, clean the exportDate of one exported today. If the export has been modified
     * returns Mono.empty() in order to include that initiative in the export stream, else returns the {@link RewardOrganizationExport}
     * as it is.
     */
    private Mono<RewardOrganizationExport> cleanTodayExport(RewardOrganizationExport x, LocalDate notificationDateToSearch) {
        LocalDate now = LocalDate.now();

        if (now.isEqual(x.getExportDate())
                && isForcedFutureExport(notificationDateToSearch)
            //TODO check status
        ) {
            log.info("[REWARD_ORGANIZATION_EXPORT][TEST] Cleaning export having id {}", x.getId());

            x.setExportDate(now.minusDays(2));
            return rewardOrganizationExportsRepository.save(x)
                    .then(Mono.empty());
        }

        return Mono.just(x);
    }

    /**
     * For functional tests purposes, clean the {@link RewardsNotification}s exported today.
     */
    private Mono<RewardsNotification> cleanAlreadyExportedNotifications(RewardsNotification n, LocalDate notificationDateToSearch) {
        if (isForcedFutureExport(notificationDateToSearch)
            && n.getExportDate() != null) {
            log.info("[REWARD_ORGANIZATION_EXPORT][TEST] Resetting notification having id {} and notificationDate {}",
                    n.getId(), n.getNotificationDate());

            n.setExportId(null);
            n.setStatus(RewardNotificationStatus.TO_SEND);

            return rewardsNotificationRepository.save(n);
        }

        return Mono.just(n);
    }

    private boolean isForcedFutureExport(LocalDate notificationDateToSearch) {
        return notificationDateToSearch.isAfter(LocalDate.now());
    }
    //endregion
}
