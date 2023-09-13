package it.gov.pagopa.reward.notification.service.csv.out.retrieve;

import it.gov.pagopa.reward.notification.dto.mapper.IbanOutcomeDTO2RewardIbanMapper;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardIbanRepository;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.csv.RewardNotificationNotifierService;
import it.gov.pagopa.reward.notification.utils.ExportCsvConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
public class Iban2NotifyRetrieverServiceImpl implements Iban2NotifyRetrieverService {

    private final RewardIbanRepository ibanRepository;
    private final RewardsNotificationRepository rewardsNotificationRepository;
    private final RewardNotificationNotifierService errorNotifierService;

    public Iban2NotifyRetrieverServiceImpl(RewardIbanRepository ibanRepository, RewardsNotificationRepository rewardsNotificationRepository, RewardNotificationNotifierService errorNotifierService) {
        this.ibanRepository = ibanRepository;
        this.rewardsNotificationRepository = rewardsNotificationRepository;
        this.errorNotifierService = errorNotifierService;
    }

    @Override
    public Mono<RewardsNotification> retrieveIban(RewardsNotification reward) {
        return ibanRepository.findById(IbanOutcomeDTO2RewardIbanMapper.buildId(reward))
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("[REWARD_NOTIFICATION_EXPORT_CSV] Cannot find iban related to beneficiary {} ({}) and initiative {}",
                            reward.getBeneficiaryId(), reward.getBeneficiaryType(), reward.getInitiativeId());
                    reward.setStatus(RewardNotificationStatus.ERROR);
                    reward.setResultCode(ExportCsvConstants.EXPORT_REJECTION_REASON_IBAN_NOT_FOUND);
                    reward.setRejectionReason(ExportCsvConstants.EXPORT_REJECTION_REASON_IBAN_NOT_FOUND);
                    return rewardsNotificationRepository.save(reward)
                            .flatMap(rn -> {
                                rn.setFeedbackDate(LocalDateTime.now());
                                return errorNotifierService.notify(rn, 0L);
                            })
                            .then(Mono.empty());
                }))
                .doOnNext(iban -> {
                    log.debug("[REWARD_NOTIFICATION_EXPORT_CSV] Iban retrieved for beneficiary {} ({}) and initiative {}",
                            reward.getBeneficiaryId(), reward.getBeneficiaryType(), reward.getInitiativeId());
                    reward.setIban(iban.getIban());
                    reward.setCheckIbanResult(iban.getCheckIbanOutcome());
                })
                .map(x -> reward)

                .onErrorResume(e -> {
                    log.error("[REWARD_NOTIFICATION_EXPORT_CSV] Something gone wrong while searching beneficiaryId-initiativeId iban", e);
                    return Mono.empty();
                });
    }
}
