package it.gov.pagopa.reward.notification.service.rewards.evaluate.notify;

import it.gov.pagopa.reward.notification.dto.mapper.RewardsNotificationMapper;
import it.gov.pagopa.reward.notification.dto.rule.TimeParameterDTO;
import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.enums.DepositType;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;

@Slf4j
@Service
public class RewardNotificationTemporalHandlerServiceImpl extends BaseRewardNotificationHandlerService implements RewardNotificationHandlerService {

    private final DayOfWeek weeklyNotificationDay;

    public RewardNotificationTemporalHandlerServiceImpl(
            @Value("${app.rewards-notification.weekly-notification-day}") DayOfWeek weeklyNotificationDay,
            RewardsNotificationRepository rewardsNotificationRepository, RewardsNotificationMapper mapper) {
        super(rewardsNotificationRepository, mapper);
        this.weeklyNotificationDay=weeklyNotificationDay;
    }

    @Override
    public Mono<RewardsNotification> handle(RewardTransactionDTO trx, RewardNotificationRule rule, Reward reward) {
        LocalDate notificationDate = calculateNotificationDate(LocalDate.now(), rule);
        String notificationId = buildNotificationId(trx, rule, notificationDate);

        return rewardsNotificationRepository.findById(notificationId)
                .flatMap(r -> {
                    if (RewardNotificationStatus.TO_SEND.equals(r.getStatus())) {
                        return Mono.just(r);
                    } else {
                        log.info("[REWARD_NOTIFICATION] Found exported notification even if scheduled for the future. Probably manually changed: {} having status {}", r.getId(), r.getStatus());
                        String beneficiaryId = getBeneficiaryId(trx, rule);
                        return rewardsNotificationRepository.findByBeneficiaryIdAndInitiativeIdAndNotificationDateAndStatusAndOrdinaryIdIsNull(beneficiaryId, rule.getInitiativeId(), notificationDate, RewardNotificationStatus.TO_SEND)
                                .switchIfEmpty(
                                        createNewNotificationWithProgressiveId(trx, rule, notificationDate, notificationId)
                                )
                                .last();
                    }
                })
                .switchIfEmpty(createNewNotification(trx, rule, notificationDate, notificationId))
                .doOnNext(n -> updateReward(trx, rule, reward, n));
    }

    public LocalDate calculateNotificationDate(LocalDate startDate, RewardNotificationRule rule) {
        if(rule.getTimeParameter().getTimeType() == null){
            throw new IllegalArgumentException("[REWARD_NOTIFICATION] Invalid timeType configured for the rule: %s".formatted(rule));
        }
        return switch (rule.getTimeParameter().getTimeType()){
            case DAILY -> startDate.plusDays(1);
            case WEEKLY -> startDate.with(TemporalAdjusters.next(weeklyNotificationDay));
            case MONTHLY -> startDate.with(TemporalAdjusters.firstDayOfNextMonth());
            case QUARTERLY -> startDate.withDayOfMonth(1).withMonth((startDate.get(IsoFields.QUARTER_OF_YEAR)*3)).plusMonths(1);
            case CLOSED -> {
                LocalDate nextDate = startDate.plusDays(1);
                yield nextDate.compareTo(rule.getEndDate()) > 0 ? nextDate : rule.getEndDate().plusDays(1);
            }
        };
    }

    private String buildNotificationId(RewardTransactionDTO trx, RewardNotificationRule rule, LocalDate notificationDate) {
        return "%s_%s_%s".formatted(
                getBeneficiaryId(trx, rule),
                rule.getInitiativeId(),
                notificationDate.format(Utils.FORMATTER_DATE)
        );
    }

    @Override
    public DepositType calcDepositType(RewardNotificationRule rule, Reward reward) {
        return  TimeParameterDTO.TimeTypeEnum.CLOSED.equals(rule.getTimeParameter().getTimeType()) ? DepositType.FINAL :
                super.calcDepositType(rule, reward);
    }
}
