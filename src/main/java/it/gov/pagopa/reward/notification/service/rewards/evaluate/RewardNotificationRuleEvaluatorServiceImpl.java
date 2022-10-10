package it.gov.pagopa.reward.notification.service.rewards.evaluate;

import it.gov.pagopa.reward.notification.dto.mapper.RewardMapper;
import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.Rewards;
import it.gov.pagopa.reward.notification.service.ErrorNotifierService;
import it.gov.pagopa.reward.notification.service.rewards.RewardsService;
import it.gov.pagopa.reward.notification.service.rule.RewardNotificationRuleService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class RewardNotificationRuleEvaluatorServiceImpl implements RewardNotificationRuleEvaluatorService {

    private final RewardNotificationRuleService rewardNotificationRuleService;
    private final RewardNotificationUpdateService rewardNotificationUpdateService;
    private final RewardMapper rewardMapper;
    private final RewardsService rewardsService;
    private final ErrorNotifierService errorNotifierService;

    public RewardNotificationRuleEvaluatorServiceImpl(RewardNotificationRuleService rewardNotificationRuleService, RewardNotificationUpdateService rewardNotificationUpdateService, RewardMapper rewardMapper, RewardsService rewardsService, ErrorNotifierService errorNotifierService) {
        this.rewardNotificationRuleService = rewardNotificationRuleService;
        this.rewardNotificationUpdateService = rewardNotificationUpdateService;
        this.rewardMapper = rewardMapper;
        this.rewardsService = rewardsService;
        this.errorNotifierService = errorNotifierService;
    }

    @Override
    public Mono<Rewards> retrieveAndEvaluate(String initiativeId, Reward reward, RewardTransactionDTO trx, Message<String> message) {
        return rewardNotificationRuleService.findById(initiativeId)
                .flatMap(rule ->
                        rewardNotificationUpdateService.configureRewardNotification(trx, rule, reward)
                                .map(notificationId -> Pair.of(rule, notificationId))
                                .defaultIfEmpty(Pair.of(rule, null))
                                .doOnNext(rule2NotificationId -> {
                                    if (!StringUtils.hasText(rule2NotificationId.getValue())) {
                                        String errorMsg = "[REWARD_NOTIFICATION] Cannot configure notificationId for reward: %s_%s"
                                                .formatted(trx.getId(), initiativeId);
                                        errorNotifierService.notifyRewardResponse(message, errorMsg, true, new IllegalStateException(errorMsg));
                                    }
                                })
                )

                .switchIfEmpty(Mono.fromSupplier(() -> {
                    String errorMsg = "[REWARD_NOTIFICATION] Cannot find initiative having id: %s".formatted(initiativeId);
                    errorNotifierService.notifyRewardResponse(message, errorMsg, true, new IllegalStateException(errorMsg));
                    return Pair.of(null, null);
                }))

                .map(rule2NotificationId -> {
                    RewardNotificationRule rule = rule2NotificationId.getKey();
                    String notificationId = rule2NotificationId.getValue();

                    Rewards r = rewardMapper.apply(initiativeId, reward, trx, rule, notificationId);

                    log.trace("[REWARD_NOTIFICATION] Reward processed ({}) and resulted into status {} and notificationId {}", r.getId(), r.getStatus(), notificationId);
                    return r;
                })
                .flatMap(rewardsService::save);
    }
}
