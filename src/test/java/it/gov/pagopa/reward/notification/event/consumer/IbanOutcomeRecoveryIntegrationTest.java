package it.gov.pagopa.reward.notification.event.consumer;

import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.dto.iban.IbanOutcomeDTO;
import it.gov.pagopa.reward.notification.dto.rule.AccumulatedAmountDTO;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardNotificationRule;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardIbanRepository;
import it.gov.pagopa.reward.notification.repository.RewardNotificationRuleRepository;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.service.iban.RewardIbanService;
import it.gov.pagopa.reward.notification.test.fakers.RewardNotificationRuleFaker;
import it.gov.pagopa.reward.notification.test.fakers.RewardsNotificationFaker;
import it.gov.pagopa.reward.notification.utils.ExportCsvConstants;
import it.gov.pagopa.reward.notification.utils.IbanConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static it.gov.pagopa.reward.notification.event.consumer.IbanOutcomeConsumerConfigTest.waitForIbanStoreChanged;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.reward.notification.service.iban.outcome.*=WARN",
})
class IbanOutcomeRecoveryIntegrationTest extends BaseIntegrationTest {

    private final int totalIban = 16;

    private static final String INITIATIVEID = "INITIATIVEID%s";
    private static final String USERID = "USERID%s";
    @Autowired
    private RewardsNotificationRepository rewardsNotificationRepository;
    @Autowired
    private RewardNotificationRuleRepository notificationRuleRepository;
    @Autowired
    private RewardIbanService rewardIbanService;
    @Autowired
    private RewardIbanRepository rewardIbanRepository;

    private List<IbanOutcomeDTO> ibanOutcomeDTOList;
    private List<RewardsNotification> rewardsNotificationList;
    private List<RewardNotificationRule> notificationRuleList;

    private final LocalDate today = LocalDate.now();


    @BeforeEach
    void prepareTestData() {
        ibanOutcomeDTOList = new ArrayList<>();
        rewardsNotificationList = new ArrayList<>();
        notificationRuleList = new ArrayList<>();

        /*
        ERROR - [0,7]
        [0,3] status ERROR and rejectionReason IBAN_NOT_FOUND
        [4,7] status ERROR only
         */
        ibanOutcomeDTOList.addAll(IntStream.rangeClosed(0, 7).mapToObj(this::mockIbanOutcome).toList());

        rewardsNotificationList.addAll(
                Objects.requireNonNull(
                        rewardsNotificationRepository.saveAll(
                                        IntStream.rangeClosed(0, 7).mapToObj(i -> {
                                            RewardsNotification x = RewardsNotificationFaker.mockInstance(i, INITIATIVEID.formatted(i), today);
                                            x.setStatus(RewardNotificationStatus.ERROR);
                                            x.setExportId(null);
                                            if (i < 4) {
                                                x.setRejectionReason(ExportCsvConstants.EXPORT_REJECTION_REASON_IBAN_NOT_FOUND);
                                            }

                                            return x;
                                        }).toList())
                                .collectList().block()
                ));

        /*
        COMPLETED_KO - [8,15]
        [8,11] recover a ordinary
        [12,15] recover a recovery
         */
        ibanOutcomeDTOList.addAll(IntStream.rangeClosed(8, 15).mapToObj(this::mockIbanOutcome).toList());

        rewardsNotificationList.addAll(
                Objects.requireNonNull(
                        rewardsNotificationRepository.saveAll(
                                        IntStream.rangeClosed(8, 15).mapToObj(i -> {
                                            RewardsNotification x = RewardsNotificationFaker.mockInstance(i, INITIATIVEID.formatted(i), today);
                                            x.setStatus(RewardNotificationStatus.COMPLETED_KO);
                                            if (i > 11) {
                                                x.setOrdinaryId(x.getId());
                                                x.setOrdinaryExternalId(x.getExternalId());
                                                x.setId(getRemedialNotificationId(x.getId()));
                                                x.setExternalId(getRemedialNotificationId(x.getExternalId()));
                                            }
                                            return x;
                                        }).toList())
                                .collectList().block()
                ));


        notificationRuleList = notificationRuleRepository.saveAll(
                        IntStream.rangeClosed(0, 15).mapToObj(i -> {
                            RewardNotificationRule x = RewardNotificationRuleFaker.mockInstance(i);
                            x.setInitiativeId(INITIATIVEID.formatted(i));

                            switch (i % 4) {
                                case 0 -> // expired
                                        x.setEndDate(today.minusDays(5));
                                case 1 -> { // timeParameter
                                    x.setEndDate(today.plusDays(5));
                                    x.setAccumulatedAmount(null);
                                }
                                case 2 -> { // budgetExhausted
                                    x.setEndDate(today.plusDays(5));
                                    x.setTimeParameter(null);
                                    x.setAccumulatedAmount(AccumulatedAmountDTO.builder()
                                            .accumulatedType(AccumulatedAmountDTO.AccumulatedTypeEnum.BUDGET_EXHAUSTED).build());
                                }
                                case 3 -> { // threshold
                                    x.setEndDate(today.plusDays(5));
                                    x.setTimeParameter(null);
                                }
                            }

                            return x;
                        }).toList())
                .collectList().block();
    }

    @AfterEach
    void cleanUp() {
        for (IbanOutcomeDTO iban : ibanOutcomeDTOList) {
            rewardIbanService.deleteIban(iban).block();
        }

        rewardsNotificationRepository.deleteAll(rewardsNotificationList).block();
        for (RewardsNotification r : rewardsNotificationList) {
            rewardsNotificationRepository.deleteById(getRemedialNotificationId(r.getId())).block();
        }

        notificationRuleRepository.deleteAll(notificationRuleList).block();
    }

    @Test
    void test() {
        // notificationId = USERID%d_INITIATIVEID%d_today.format(Utils.FORMATTER_DATE)

        ibanOutcomeDTOList.forEach(p -> publishIntoEmbeddedKafka(topicIbanOutcome, null, p.getUserId().concat(p.getInitiativeId()), p));

        waitForIbanStoreChanged(totalIban, rewardIbanRepository);

        for (RewardsNotification r : rewardsNotificationList) {
            RewardsNotification recovered = rewardsNotificationRepository.findById(r.getId()).block();
            Assertions.assertNotNull(recovered);

            int i = getIndexFromNotificationId(recovered.getId());
            if (i >= 0 && i <= 3) { // status ERROR rejectionReason IBAN_NOT_FOUND - [0,3]
                checkRecoveredErrorStatus(i, recovered);
            }
            else if (i >= 4 && i <= 7){ // status ERROR rejectionReason null - [4,7]
                // the recovered notification should be identical to the r one, since it should be ignored by the RecoveryService
                Assertions.assertEquals(r, recovered);
            }

            else if (i >= 8 && i <= 15) { // COMPLETED_KO - [8,15]
                checkRecoveredCompletedKoStatus(i, r, recovered);
            }

            else {
                throw new IllegalStateException("[IBAN_RECOVERY_TEST] Unexpected index: %d".formatted(i));
            }

        }
    }

    private IbanOutcomeDTO mockIbanOutcome(int bias) {
        return IbanOutcomeDTO.builder()
                .userId(USERID.formatted(bias))
                .initiativeId(INITIATIVEID.formatted(bias))
                .iban("IBAN%s".formatted(bias))
                .status(IbanConstants.STATUS_OK)
                .build();
    }

    private String getRemedialNotificationId(String id) {
        String RECOVERY_ID_SUFFIX = "_recovery-";

        String[] idSplit = id.split(RECOVERY_ID_SUFFIX);
        if (idSplit.length == 2) {
            int nextRecoveryProgressiveId = Integer.parseInt(idSplit[1]) + 1;
            return "%s%s%d".formatted(idSplit[0], RECOVERY_ID_SUFFIX, nextRecoveryProgressiveId);
        } else {
            return "%s%s%d".formatted(id, RECOVERY_ID_SUFFIX, 1);
        }

    }

    private int getIndexFromNotificationId(String id) {
        String idWithoutUserId = id.substring(6);
        String[] idSplit = idWithoutUserId.split("_[a-zA-Z]");
        return Integer.parseInt(idSplit[0]);
    }

    private void checkRecoveredErrorStatus(int i, RewardsNotification recovered) {
        Assertions.assertEquals(RewardNotificationStatus.TO_SEND, recovered.getStatus(), "notification %s".formatted(recovered.getId()));
        Assertions.assertNull(recovered.getRejectionReason());
        Assertions.assertNull(recovered.getResultCode());
        Assertions.assertNull(recovered.getExportDate());

        checkNotificationDate(i, recovered.getNotificationDate(), recovered.getId());
    }

    private void checkRecoveredCompletedKoStatus(int i, RewardsNotification r, RewardsNotification recovered) {
        Assertions.assertEquals(getRemedialNotificationId(r.getId()), recovered.getRemedialId());
        Assertions.assertEquals(RewardNotificationStatus.RECOVERED, recovered.getStatus());

        checkRemedialNotification(i, r);
    }

    private void checkRemedialNotification(int i, RewardsNotification r) {
        RewardsNotification remedial = rewardsNotificationRepository.findById(getRemedialNotificationId(r.getId())).block();
        Assertions.assertNotNull(remedial);

        Assertions.assertEquals(getRemedialNotificationId(r.getId()), remedial.getId());
        Assertions.assertEquals(getRemedialNotificationId(r.getExternalId()), remedial.getExternalId());
        Assertions.assertEquals(i < 12 ? r.getId() : r.getOrdinaryId(), remedial.getOrdinaryId());
        Assertions.assertEquals(i < 12 ? r.getExternalId() : r.getOrdinaryExternalId(), remedial.getOrdinaryExternalId());
        Assertions.assertEquals(RewardNotificationStatus.TO_SEND, remedial.getStatus());
        Assertions.assertNull(remedial.getExportId());
        Assertions.assertNull(remedial.getExportDate());
        Assertions.assertNull(remedial.getIban());
        Assertions.assertNull(remedial.getRejectionReason());
        Assertions.assertNull(remedial.getResultCode());
        Assertions.assertNull(remedial.getFeedbackDate());
        Assertions.assertEquals(Collections.emptyList(), remedial.getFeedbackHistory());
        Assertions.assertNull(remedial.getCro());
        Assertions.assertNull(remedial.getExecutionDate());
        Assertions.assertNull(remedial.getRemedialId());

        checkNotificationDate(i, remedial.getNotificationDate(), remedial.getId());
    }

    private void checkNotificationDate(int i, LocalDate notificationDate, String id) {
        switch (i % 4) {
            case 0 -> // expired
                    Assertions.assertEquals(today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)), notificationDate, "CASE EXPIRED: %s".formatted(id));
            case 1 -> // timeParameter
                    Assertions.assertEquals(today.plusDays(1), notificationDate, "CASE TEMPORAL: %s".formatted(id));
            case 2 -> // budgetExhausted
                    Assertions.assertEquals(today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)), notificationDate, "CASE BUDGET: %s".formatted(id));
            case 3 -> // threshold
                    Assertions.assertEquals(today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)), notificationDate, "CASE THRESHOLD: %s".formatted(id));
        }
    }
}
