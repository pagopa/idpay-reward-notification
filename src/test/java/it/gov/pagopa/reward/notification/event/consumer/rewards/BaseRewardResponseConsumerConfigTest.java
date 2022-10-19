package it.gov.pagopa.reward.notification.event.consumer.rewards;

import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.dto.mapper.RewardMapper;
import it.gov.pagopa.reward.notification.dto.mapper.RewardsNotificationMapper;
import it.gov.pagopa.reward.notification.dto.rule.*;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.enums.DepositType;
import it.gov.pagopa.reward.notification.event.consumer.RefundRuleConsumerConfigTest;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.Rewards;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardNotificationRuleRepository;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.repository.RewardsRepository;
import it.gov.pagopa.reward.notification.service.LockServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.reward.notification.service.rewards.RewardsServiceImpl=WARN",
})
@Slf4j
abstract class BaseRewardResponseConsumerConfigTest extends BaseIntegrationTest {

    public static final LocalDate TODAY = LocalDate.now();
    public static final LocalDate TOMORROW = TODAY.plusDays(1);
    public static final LocalDate NEXT_WEEK= TODAY.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
    public static final LocalDate NEXT_MONTH= TODAY.with(TemporalAdjusters.firstDayOfNextMonth());
    public static final LocalDate NEXT_QUARTER= TODAY.withDayOfMonth(1).withMonth((TODAY.get(IsoFields.QUARTER_OF_YEAR)*3)).plusMonths(1);

    @Autowired
    protected RewardsRepository rewardsRepository;
    @Autowired
    protected RewardsNotificationRepository rewardsNotificationRepository;
    // TODO uncomment when rewardsOrganizationExports logic has been implemented
//    @Autowired
//    protected RewardsOrganizationExportsRepository rewardsOrganizationExportsRepository;

    @Autowired
    protected RewardNotificationRuleRepository ruleRepository;

    @Autowired
    protected LockServiceImpl lockService;
    @Autowired
    protected RewardMapper rewardMapper;
    @Autowired
    protected RewardsNotificationMapper notificationMapper;

    @AfterEach
    void checkLockBouquet() throws NoSuchFieldException, IllegalAccessException {
        final Field locksField = LockServiceImpl.class.getDeclaredField("locks");
        locksField.setAccessible(true);
        @SuppressWarnings("unchecked") Map<Integer, Semaphore> locks = (Map<Integer, Semaphore>) locksField.get(lockService);
        locks.values().forEach(l -> Assertions.assertEquals(1, l.availablePermits()));
    }

    @AfterEach
    void clearData() {
        rewardsRepository.deleteAll().block();
        rewardsNotificationRepository.deleteAll().block();
        // TODO uncomment when rewardsOrganizationExports logic has been implemented
//        rewardsOrganizationExportsRepository.deleteAll().block();
        ruleRepository.deleteAll().block();
    }

    protected long waitForRewardsStored(int n) {
        long[] countSaved = {0};
        waitFor(() -> (countSaved[0] = fetchNewRewardsStored().size()) >= n, () -> "Expected %d saved reward notification rules, read %d".formatted(n, countSaved[0]), 60, 1000);
        return countSaved[0];
    }

    protected List<Rewards> fetchNewRewardsStored() {
        return rewardsRepository.findAll().filter(r->!r.getTrxId().startsWith("ALREADY")).collectList().block();
    }

    protected List<RewardsNotification> prepare2Compare(Collection<RewardsNotification> values) {
        return values.stream()
                .sorted(Comparator.comparing(RewardsNotification::getUserId).thenComparing(RewardsNotification::getId))
                .peek(r -> {
                    Assertions.assertFalse(StringUtils.isEmpty(r.getExternalId()), "Invalid null externalId on reward notification:" + r);
                    r.setExternalId("");
                })
                .toList();
    }

    // region initiative build
    protected static final LocalDate INITIATIVE_ENDDATE = TODAY.plusDays(10);
    protected static final LocalDate INITIATIVE_ENDDATE_NEXT_DAY = INITIATIVE_ENDDATE.plusDays(1);

    protected static final String INITIATIVE_ID_NOTIFY_DAILY = "INITIATIVEID_DAILY";
    protected static final String INITIATIVE_ID_NOTIFY_WEEKLY = "INITIATIVEID_WEEKLY";
    protected static final String INITIATIVE_ID_NOTIFY_MONTHLY = "INITIATIVEID_MONTHLY";
    protected static final String INITIATIVE_ID_NOTIFY_QUARTERLY = "INITIATIVEID_QUARTERLY";
    protected static final String INITIATIVE_ID_NOTIFY_CLOSED = "INITIATIVEID_CLOSED";
    protected static final String INITIATIVE_ID_NOTIFY_CLOSED_ALREADY_EXPIRED = "INITIATIVEID_CLOSED_ALREADY_EXPIRED";
    protected static final String INITIATIVE_ID_NOTIFY_THRESHOLD = "INITIATIVEID_THRESHOLD";
    protected static final String INITIATIVE_ID_NOTIFY_EXHAUSTED = "INITIATIVEID_EXHAUSTED";

    protected void publishRewardRules() {
        List<InitiativeRefund2StoreDTO> rules = List.of(
                InitiativeRefund2StoreDTO.builder()
                        .initiativeId(INITIATIVE_ID_NOTIFY_DAILY)
                        .initiativeName("INITIATIVE_NAME_" + INITIATIVE_ID_NOTIFY_DAILY)
                        .organizationId("ORGANIZATION_ID_" + INITIATIVE_ID_NOTIFY_DAILY)
                        .organizationVat("ORGANIZATION_VAT_" + INITIATIVE_ID_NOTIFY_DAILY)
                        .general(InitiativeGeneralDTO.builder()
                                .endDate(INITIATIVE_ENDDATE)
                                .build())
                        .refundRule(InitiativeRefundRuleDTO.builder()
                                .accumulatedAmount(AccumulatedAmountDTO.builder() // ignored because configured as timed
                                        .accumulatedType(AccumulatedAmountDTO.AccumulatedTypeEnum.THRESHOLD_REACHED)
                                        .refundThreshold(BigDecimal.ONE)
                                        .build())
                                .timeParameter(TimeParameterDTO.builder()
                                        .timeType(TimeParameterDTO.TimeTypeEnum.DAILY)
                                        .build())
                                .build())
                        .build(),
                InitiativeRefund2StoreDTO.builder()
                        .initiativeId(INITIATIVE_ID_NOTIFY_WEEKLY)
                        .initiativeName("INITIATIVE_NAME_" + INITIATIVE_ID_NOTIFY_WEEKLY)
                        .organizationId("ORGANIZATION_ID_" + INITIATIVE_ID_NOTIFY_WEEKLY)
                        .organizationVat("ORGANIZATION_VAT_" + INITIATIVE_ID_NOTIFY_WEEKLY)
                        .general(InitiativeGeneralDTO.builder()
                                .endDate(INITIATIVE_ENDDATE)
                                .build())
                        .refundRule(InitiativeRefundRuleDTO.builder()
                                .timeParameter(TimeParameterDTO.builder()
                                        .timeType(TimeParameterDTO.TimeTypeEnum.WEEKLY)
                                        .build())
                                .build())
                        .build(),
                InitiativeRefund2StoreDTO.builder()
                        .initiativeId(INITIATIVE_ID_NOTIFY_MONTHLY)
                        .initiativeName("INITIATIVE_NAME_" + INITIATIVE_ID_NOTIFY_MONTHLY)
                        .organizationId("ORGANIZATION_ID_" + INITIATIVE_ID_NOTIFY_MONTHLY)
                        .organizationVat("ORGANIZATION_VAT_" + INITIATIVE_ID_NOTIFY_MONTHLY)
                        .general(InitiativeGeneralDTO.builder()
                                .endDate(INITIATIVE_ENDDATE)
                                .build())
                        .refundRule(InitiativeRefundRuleDTO.builder()
                                .timeParameter(TimeParameterDTO.builder()
                                        .timeType(TimeParameterDTO.TimeTypeEnum.MONTHLY)
                                        .build())
                                .build())
                        .build(),
                InitiativeRefund2StoreDTO.builder()
                        .initiativeId(INITIATIVE_ID_NOTIFY_QUARTERLY)
                        .initiativeName("INITIATIVE_NAME_" + INITIATIVE_ID_NOTIFY_QUARTERLY)
                        .organizationId("ORGANIZATION_ID_" + INITIATIVE_ID_NOTIFY_QUARTERLY)
                        .organizationVat("ORGANIZATION_VAT_" + INITIATIVE_ID_NOTIFY_QUARTERLY)
                        .general(InitiativeGeneralDTO.builder()
                                .endDate(INITIATIVE_ENDDATE)
                                .build())
                        .refundRule(InitiativeRefundRuleDTO.builder()
                                .timeParameter(TimeParameterDTO.builder()
                                        .timeType(TimeParameterDTO.TimeTypeEnum.QUARTERLY)
                                        .build())
                                .build())
                        .build(),
                InitiativeRefund2StoreDTO.builder()
                        .initiativeId(INITIATIVE_ID_NOTIFY_CLOSED)
                        .initiativeName("INITIATIVE_NAME_" + INITIATIVE_ID_NOTIFY_CLOSED)
                        .organizationId("ORGANIZATION_ID_" + INITIATIVE_ID_NOTIFY_CLOSED)
                        .organizationVat("ORGANIZATION_VAT_" + INITIATIVE_ID_NOTIFY_CLOSED)
                        .general(InitiativeGeneralDTO.builder()
                                .endDate(INITIATIVE_ENDDATE)
                                .build())
                        .refundRule(InitiativeRefundRuleDTO.builder()
                                .timeParameter(TimeParameterDTO.builder()
                                        .timeType(TimeParameterDTO.TimeTypeEnum.CLOSED)
                                        .build())
                                .build())
                        .build(),
                InitiativeRefund2StoreDTO.builder()
                        .initiativeId(INITIATIVE_ID_NOTIFY_CLOSED_ALREADY_EXPIRED)
                        .initiativeName("INITIATIVE_NAME_" + INITIATIVE_ID_NOTIFY_CLOSED_ALREADY_EXPIRED)
                        .organizationId("ORGANIZATION_ID_" + INITIATIVE_ID_NOTIFY_CLOSED_ALREADY_EXPIRED)
                        .organizationVat("ORGANIZATION_VAT_" + INITIATIVE_ID_NOTIFY_CLOSED_ALREADY_EXPIRED)
                        .general(InitiativeGeneralDTO.builder()
                                .endDate(TODAY.minusDays(1))
                                .build())
                        .refundRule(InitiativeRefundRuleDTO.builder()
                                .timeParameter(TimeParameterDTO.builder()
                                        .timeType(TimeParameterDTO.TimeTypeEnum.CLOSED)
                                        .build())
                                .build())
                        .build(),

                InitiativeRefund2StoreDTO.builder()
                        .initiativeId(INITIATIVE_ID_NOTIFY_THRESHOLD)
                        .initiativeName("INITIATIVE_NAME_" + INITIATIVE_ID_NOTIFY_THRESHOLD)
                        .organizationId("ORGANIZATION_ID_" + INITIATIVE_ID_NOTIFY_THRESHOLD)
                        .organizationVat("ORGANIZATION_VAT_" + INITIATIVE_ID_NOTIFY_THRESHOLD)
                        .general(InitiativeGeneralDTO.builder()
                                .endDate(INITIATIVE_ENDDATE)
                                .build())
                        .refundRule(InitiativeRefundRuleDTO.builder()
                                .accumulatedAmount(AccumulatedAmountDTO.builder() // ignored because configured as timed
                                        .accumulatedType(AccumulatedAmountDTO.AccumulatedTypeEnum.THRESHOLD_REACHED)
                                        .refundThreshold(BigDecimal.valueOf(100))
                                        .build())
                                .build())
                        .build(),
                InitiativeRefund2StoreDTO.builder()
                        .initiativeId(INITIATIVE_ID_NOTIFY_EXHAUSTED)
                        .initiativeName("INITIATIVE_NAME_" + INITIATIVE_ID_NOTIFY_EXHAUSTED)
                        .organizationId("ORGANIZATION_ID_" + INITIATIVE_ID_NOTIFY_EXHAUSTED)
                        .organizationVat("ORGANIZATION_VAT_" + INITIATIVE_ID_NOTIFY_EXHAUSTED)
                        .general(InitiativeGeneralDTO.builder()
                                .endDate(INITIATIVE_ENDDATE)
                                .build())
                        .refundRule(InitiativeRefundRuleDTO.builder()
                                .accumulatedAmount(AccumulatedAmountDTO.builder() // ignored because configured as timed
                                        .accumulatedType(AccumulatedAmountDTO.AccumulatedTypeEnum.BUDGET_EXHAUSTED)
                                        .build())
                                .build())
                        .build()
        );
        rules.forEach(i -> publishIntoEmbeddedKafka(topicInitiative2StoreConsumer, null, null, i));

        RefundRuleConsumerConfigTest.waitForInitiativeStored(rules.size(), ruleRepository);
    }
    //endregion

    protected void checkOffsets(long expectedReadMessages) {
        long timeStart = System.currentTimeMillis();
        final Map<TopicPartition, OffsetAndMetadata> srcCommitOffsets = checkCommittedOffsets(topicRewardResponse, groupIdRewardResponse, expectedReadMessages, 20, 1000);
        long timeCommitChecked = System.currentTimeMillis();

        System.out.printf("""
                        ************************
                        Time occurred to check committed offset: %d millis
                        ************************
                        Source Topic Committed Offsets: %s
                        ************************
                        """,
                timeCommitChecked - timeStart,
                srcCommitOffsets
        );
    }

    protected void assertRewards(Rewards reward, String expectedInitiativeId, String notificationId, LocalDate expectedNotificationDate, BigDecimal expectedReward, boolean notifiedAlone) {
        String errorMsg = "Unexpected result verifying reward: " + reward;

        Assertions.assertEquals(expectedInitiativeId, reward.getInitiativeId(), errorMsg);
        Assertions.assertEquals(notificationId, reward.getNotificationId(), errorMsg);
        Assertions.assertEquals(expectedReward, reward.getReward(), errorMsg);

        RewardsNotification notification = rewardsNotificationRepository.findById(notificationId).block();
        Assertions.assertNotNull(notification, errorMsg);
        if(notifiedAlone){
            Assertions.assertEquals(List.of(reward.getTrxId()), notification.getTrxIds(), errorMsg);
        } else {
            Assertions.assertTrue(notification.getTrxIds().contains(reward.getTrxId()), errorMsg);
        }
        Assertions.assertEquals(expectedNotificationDate, notification.getNotificationDate(), errorMsg);
    }

    protected RewardsNotification buildRewardsNotificationExpected(String notificationId, LocalDate notificationDate, RewardTransactionDTO trx, long progressive, String initiativeId, long rewardCents, DepositType depositType, RewardsNotification n) {
        if(n ==null){
            RewardNotificationRule rule = RewardNotificationRule.builder()
                    .initiativeId(initiativeId)
                    .initiativeName("INITIATIVE_NAME_" + initiativeId)
                    .organizationId("ORGANIZATION_ID_" + initiativeId)
                    .organizationFiscalCode("ORGANIZATION_VAT_" + initiativeId)
                    .build();
            n = notificationMapper.apply(notificationId, notificationDate, progressive, trx, rule);
        }
        n.setRewardCents(n.getRewardCents() + rewardCents);
        n.getTrxIds().add(trx.getId());
        n.setDepositType(depositType);
        return n;
    }

}