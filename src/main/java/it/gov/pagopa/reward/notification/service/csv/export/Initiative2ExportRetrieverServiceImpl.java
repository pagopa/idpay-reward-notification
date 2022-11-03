package it.gov.pagopa.reward.notification.service.csv.export;

import com.mongodb.DuplicateKeyException;
import it.gov.pagopa.reward.notification.enums.ExportStatus;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.repository.RewardNotificationRuleRepository;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationExportsRepository;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.utils.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Service
public class Initiative2ExportRetrieverServiceImpl implements Initiative2ExportRetrieverService {

    private final String exportBasePath;

    private final RewardOrganizationExportsRepository rewardOrganizationExportsRepository;
    private final RewardsNotificationRepository rewardsNotificationRepository;
    private final RewardNotificationRuleRepository rewardNotificationRuleRepository;


    public Initiative2ExportRetrieverServiceImpl(
            @Value("${app.csv.export.storage.base-path}") String exportBasePath,
            RewardOrganizationExportsRepository rewardOrganizationExportsRepository, RewardsNotificationRepository rewardsNotificationRepository, RewardNotificationRuleRepository rewardNotificationRuleRepository) {
        this.rewardOrganizationExportsRepository = rewardOrganizationExportsRepository;
        this.rewardsNotificationRepository = rewardsNotificationRepository;
        this.exportBasePath = exportBasePath;
        this.rewardNotificationRuleRepository = rewardNotificationRuleRepository;
    }
//TODO put some logs
    @Override
    public Mono<RewardOrganizationExport> retrieve() {
        return rewardOrganizationExportsRepository.reserveExport()
                .switchIfEmpty(retrieveNewExports());
    }

    private Mono<RewardOrganizationExport> retrieveNewExports() {
        return rewardsNotificationRepository.findInitiatives2Notify()
                .flatMap(this::configureExport)
                .collectList()
                .flatMap(x -> rewardOrganizationExportsRepository.reserveExport());
    }

    private Mono<RewardOrganizationExport> configureExport(String initiativeId) {
        LocalDate now = LocalDate.now();

        return rewardNotificationRuleRepository.findById(initiativeId)
                .flatMap(rule -> rewardOrganizationExportsRepository.count(Example.of(RewardOrganizationExport.builder()
                                .initiativeId(initiativeId)
                                .notificationDate(now)
                                .build()))
                        .defaultIfEmpty(0L)
                        .map(progressive -> buildNewRewardOrganizationExportEntity(rule, now, progressive+1)))
                .flatMap(e-> {
                    try{
                        return rewardOrganizationExportsRepository.configureNewExport(e);
                    } catch (DuplicateKeyException duplicateKeyException){
                        return Mono.empty();// TODO test this
                    }
                });
    }

    protected RewardOrganizationExport buildNewRewardOrganizationExportEntity(RewardNotificationRule rule, LocalDate now, long progressive) {
        String nowFormatted = Utils.FORMATTER_DATE.format(now);
        return RewardOrganizationExport.builder()
                .id("%s_%s.%d".formatted(rule.getInitiativeId(), nowFormatted, progressive))
                .filePath("%s/%s/%s/%s".formatted(
                        exportBasePath,
                        rule.getOrganizationId(),
                        rule.getInitiativeId(),
                        buildExportFileName(rule, nowFormatted, progressive)
                ))
                .initiativeId(rule.getInitiativeId())
                .initiativeName(rule.getInitiativeName())
                .organizationId(rule.getOrganizationId())
                .notificationDate(now)
                .status(ExportStatus.TODO)
                .rewardsExportedCents(0L)
                .rewardsResultsCents(0L)
                .rewardNotified(0L)
                .rewardsResulted(0L)
                .build();
    }

    private String buildExportFileName(RewardNotificationRule rule, String dateFormatted, long progressive) {
        return "%s_%s.%d.zip".formatted(
                escapeRuleName(rule.getInitiativeName()),
                dateFormatted, progressive);
    }

    private String escapeRuleName(String initiativeName) {
        return initiativeName.replaceAll("\\W", "").substring(0, 10);
    }
}
