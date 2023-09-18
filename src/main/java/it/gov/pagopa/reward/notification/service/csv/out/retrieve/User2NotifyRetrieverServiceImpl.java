package it.gov.pagopa.reward.notification.service.csv.out.retrieve;

import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.model.User;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.UserService;
import it.gov.pagopa.reward.notification.service.csv.RewardNotificationNotifierService;
import it.gov.pagopa.reward.notification.utils.ExportCsvConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
public class User2NotifyRetrieverServiceImpl implements User2NotifyRetrieverService {

    private final UserService userService;
    private final RewardsNotificationRepository rewardsNotificationRepository;
    private final RewardNotificationNotifierService errorNotifierService;

    public User2NotifyRetrieverServiceImpl(UserService userService, RewardsNotificationRepository rewardsNotificationRepository, RewardNotificationNotifierService errorNotifierService) {
        this.userService = userService;
        this.rewardsNotificationRepository = rewardsNotificationRepository;
        this.errorNotifierService = errorNotifierService;
    }

    @Override
    public Mono<Pair<RewardsNotification, User>> retrieveUser(RewardsNotification reward) {
        return userService.getUserInfo(reward.getBeneficiaryId())
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("[REWARD_NOTIFICATION_EXPORT_CSV] Cannot find fiscalCode related to user {}", reward.getBeneficiaryId());

                    reward.setStatus(RewardNotificationStatus.ERROR);
                    reward.setResultCode(ExportCsvConstants.EXPORT_REJECTION_REASON_CF_NOT_FOUND);
                    reward.setRejectionReason(ExportCsvConstants.EXPORT_REJECTION_REASON_CF_NOT_FOUND);
                    return rewardsNotificationRepository.save(reward)
                            .flatMap(rn -> {
                                rn.setFeedbackDate(LocalDateTime.now());
                                return errorNotifierService.notify(rn, 0L);
                            })
                            .then(Mono.empty());
                }))
                .map(user -> {
                    log.debug("[REWARD_NOTIFICATION_EXPORT_CSV] fiscalCode related to user {} retrieved", reward.getBeneficiaryId());

                    return Pair.of(reward, user);
                })

                .onErrorResume(e -> {
                    log.error("[REWARD_NOTIFICATION_EXPORT_CSV] Something gone wrong while searching userId", e);
                    return Mono.empty();
                });
    }
}
