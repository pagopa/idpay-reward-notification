package it.gov.pagopa.reward.notification.service.rule;

import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.repository.RewardNotificationRuleRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class RewardNotificationRuleServiceImpl implements RewardNotificationRuleService{

    private final RewardNotificationRuleRepository rewardNotificationRuleRepository;

    public RewardNotificationRuleServiceImpl(RewardNotificationRuleRepository rewardNotificationRuleRepository) {
        this.rewardNotificationRuleRepository = rewardNotificationRuleRepository;
    }

    @Override
    public Mono<RewardNotificationRule> findById(String initiativeId) {
        return rewardNotificationRuleRepository.findById(initiativeId).cache();
    }

    @Override
    public Mono<RewardNotificationRule> save(RewardNotificationRule rewardNotificationRule) {
        return rewardNotificationRuleRepository.save(rewardNotificationRule);
    }
}
