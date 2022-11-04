package it.gov.pagopa.reward.notification.service.csv.export.retrieve;

import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.model.User;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.UserService;
import it.gov.pagopa.reward.notification.service.utils.ExportCsvConstants;
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

    public User2NotifyRetrieverServiceImpl(UserService userService, RewardsNotificationRepository rewardsNotificationRepository) {
        this.userService = userService;
        this.rewardsNotificationRepository = rewardsNotificationRepository;
    }

    @Override
    public Mono<Pair<RewardsNotification, User>> retrieveUser(RewardsNotification reward) {
        return userService.getUserInfo(reward.getUserId())
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("[REWARD_NOTIFICATION_EXPORT_CSV] Cannot find fiscalCode related to user {}", reward.getUserId());

                    reward.setStatus(RewardNotificationStatus.ERROR);
                    reward.setRejectionReason(ExportCsvConstants.EXPORT_REJECTION_REASON_CF_NOT_FOUND);
                    reward.setExportDate(LocalDateTime.now());
                    return rewardsNotificationRepository.save(reward)
                                    .then(Mono.empty());
                }))
                .map(user -> {
                    log.debug("[REWARD_NOTIFICATION_EXPORT_CSV] fiscalCode related to user {} retrieved", reward.getUserId());

                    return Pair.of(reward, user);
                });
    }
}
