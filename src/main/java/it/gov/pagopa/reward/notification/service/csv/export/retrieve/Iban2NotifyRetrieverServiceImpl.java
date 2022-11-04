package it.gov.pagopa.reward.notification.service.csv.export.retrieve;

import it.gov.pagopa.reward.notification.dto.mapper.IbanOutcomeDTO2RewardIbanMapper;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardIbanRepository;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.utils.ExportCsvConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class Iban2NotifyRetrieverServiceImpl implements Iban2NotifyRetrieverService {

    private final RewardIbanRepository ibanRepository;
    private final RewardsNotificationRepository rewardsNotificationRepository;

    public Iban2NotifyRetrieverServiceImpl(RewardIbanRepository ibanRepository, RewardsNotificationRepository rewardsNotificationRepository) {
        this.ibanRepository = ibanRepository;
        this.rewardsNotificationRepository = rewardsNotificationRepository;
    }

    @Override
    public Mono<RewardsNotification> retrieveIban(RewardsNotification reward) {
        return ibanRepository.findById(IbanOutcomeDTO2RewardIbanMapper.buildId(reward))
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("[REWARD_NOTIFICATION_EXPORT_CSV] Cannot find iban related to user {} and initiative {}", reward.getUserId(), reward.getInitiativeId());
                    reward.setStatus(RewardNotificationStatus.ERROR);
                    reward.setRejectionReason(ExportCsvConstants.EXPORT_REJECTION_REASON_IBAN_NOT_FOUND);
                    return rewardsNotificationRepository.save(reward)
                                    .then(Mono.empty());
                }))
                .doOnNext(iban -> {
                    log.debug("[REWARD_NOTIFICATION_EXPORT_CSV] Iban retrieved for user {} and initiative {}", reward.getUserId(), reward.getInitiativeId());

                    reward.setIban(iban.getIban());
                    reward.setCheckIbanResult(iban.getCheckIbanOutcome());
                })
                .map(x -> reward);
    }
}
