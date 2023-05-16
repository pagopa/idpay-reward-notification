package it.gov.pagopa.reward.notification.service.iban.outcome.recovery;

import it.gov.pagopa.reward.notification.model.RewardIban;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class RecoverIbanKoServiceImpl implements RecoverIbanKoService {

    private final NeverExportedDiscardedRewardNotificationService neverExportedService;
    private final CompletedKoDiscardedRewardNotificationService completedKoService;

    public RecoverIbanKoServiceImpl(NeverExportedDiscardedRewardNotificationService neverExportedService, CompletedKoDiscardedRewardNotificationService completedKoService) {
        this.neverExportedService = neverExportedService;
        this.completedKoService = completedKoService;
    }

    @Override
    public Mono<RewardIban> recover(RewardIban rewardIban) {

        return neverExportedService.handleNeverExportedDiscardedRewardNotification(rewardIban)
                .thenMany(completedKoService.handleCompletedKoDiscardedRewardNotification(rewardIban))
                .then(Mono.just(rewardIban))
                .onErrorResume(e -> {
                    log.error("[REWARD_NOTIFICATION_IBAN_OUTCOME] Something went wrong while recovering discarded rewardNotification related to userId {} and initiativeId {}",
                            rewardIban.getUserId(), rewardIban.getInitiativeId(), e);
                    return Mono.just(rewardIban);
                });
    }
}
