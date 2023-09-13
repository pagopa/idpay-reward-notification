package it.gov.pagopa.reward.notification.service.csv.out.retrieve;

import it.gov.pagopa.reward.notification.connector.rest.MerchantRestClient;
import it.gov.pagopa.reward.notification.dto.rest.MerchantDetailDTO;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.csv.RewardNotificationNotifierService;
import it.gov.pagopa.reward.notification.utils.ExportCsvConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@Slf4j
public class Merchant2NotifyRetrieverServiceImpl implements Merchant2NotifyRetrieverService {

    private final MerchantRestClient merchantRestClient;
    private final RewardsNotificationRepository rewardsNotificationRepository;
    private final RewardNotificationNotifierService errorNotifierService;

    public Merchant2NotifyRetrieverServiceImpl(MerchantRestClient merchantRestClient, RewardsNotificationRepository rewardsNotificationRepository, RewardNotificationNotifierService errorNotifierService) {
        this.merchantRestClient = merchantRestClient;
        this.rewardsNotificationRepository = rewardsNotificationRepository;
        this.errorNotifierService = errorNotifierService;
    }

    @Override
    public Mono<Pair<RewardsNotification, MerchantDetailDTO>> retrieve(RewardsNotification reward) {
        return merchantRestClient.getMerchant(reward.getBeneficiaryId(), reward.getOrganizationId(),reward.getInitiativeId())
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("[REWARD_NOTIFICATION_EXPORT_CSV] Cannot find merchant having id {} related to initiative {}",
                            reward.getBeneficiaryId(), reward.getInitiativeId());

                    reward.setStatus(RewardNotificationStatus.ERROR);
                    reward.setResultCode(ExportCsvConstants.EXPORT_REJECTION_REASON_MERCHANT_NOT_FOUND);
                    reward.setRejectionReason(ExportCsvConstants.EXPORT_REJECTION_REASON_MERCHANT_NOT_FOUND);
                    return rewardsNotificationRepository.save(reward)
                            .flatMap(rn -> {
                                rn.setFeedbackDate(LocalDateTime.now());
                                return errorNotifierService.notify(rn, 0L);
                            })
                            .then(Mono.empty());
                }))
                .map(merchant -> {
                    log.debug("[REWARD_NOTIFICATION_EXPORT_CSV] fiscalCode related to merchant {} retrieved", reward.getBeneficiaryId());

                    return Pair.of(reward, merchant);
                })

                .onErrorResume(e -> {
                    log.error("[REWARD_NOTIFICATION_EXPORT_CSV] Something gone wrong while searching merchantId", e);
                    return Mono.empty();
                });
    }

}
