package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.dto.controller.detail.ExportDetailFilter;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.utils.NotificationConstants;
import org.apache.commons.lang3.ObjectUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class RewardsNotificationRepositoryExtendedTest extends BaseIntegrationTest {

    public static final LocalDate NOTIFICATION_DATE = LocalDate.now();
    private static final String CRO = "ZYXWVU12345";
    private static final List<RewardNotificationStatus> STATUS_WITH_EXECUTION_DATE = List.of(RewardNotificationStatus.COMPLETED_OK, RewardNotificationStatus.COMPLETED_KO);
    public static final String INITIATIVEIDDETAIL = "INITIATIVEIDDETAIL";
    private static List<RewardsNotification> NOTIFICATIONS_WITH_EXPORTID = new ArrayList<>();
    private static List<RewardsNotification> NOTIFICATIONS_DETAIL = new ArrayList<>();
    private static final ExportDetailFilter EMPTY_FILTERS = new ExportDetailFilter();
    private static final Pageable DEFAULT_PAGEABLE = PageRequest.of(0, 10);
    @Autowired
    private RewardsNotificationRepository repository;

    @Value("${app.csv.export.day-before}")
    private Integer dayBefore;

    private List<RewardsNotification> testData;

    @BeforeEach
    void createTestData(){
        testData = new ArrayList<>();

        // 0: useCase TO_SEND without notificationDate
        addFindInitiatives2NotifyUseCase(RewardNotificationStatus.TO_SEND, null);
        // 1: useCase TO_SEND with past notificationDate
        addFindInitiatives2NotifyUseCase(RewardNotificationStatus.TO_SEND, dayBefore + 1);
        // 2: useCase TO_SEND with future notificationDate
        addFindInitiatives2NotifyUseCase(RewardNotificationStatus.TO_SEND, -1);
        // 3: useCase TO_SEND with expected notificationDate
        addFindInitiatives2NotifyUseCase(RewardNotificationStatus.TO_SEND, dayBefore);
        // 4: useCase TO_SEND today
        addFindInitiatives2NotifyUseCase("INITIATIVEIDNOTIFIEDTWICE", RewardNotificationStatus.TO_SEND, 0);
        // 5: useCase TO_SEND today on same initiativeId, but already exported
        addFindInitiatives2NotifyUseCase("INITIATIVEIDNOTIFIEDTWICE", RewardNotificationStatus.EXPORTED, 0);
        // 6: useCase TO_SEND today on same initiativeId
        addFindInitiatives2NotifyUseCase("INITIATIVEIDNOTIFIEDTWICE", RewardNotificationStatus.TO_SEND, 0);
        // 7: useCase EXPORTED with expected notificationDate
        addFindInitiatives2NotifyUseCase(RewardNotificationStatus.EXPORTED, dayBefore);
        // 8: useCase TO_SEND with expected notificationDate, but excluded by parameters
        addFindInitiatives2NotifyUseCase("INITIATIVEEXCLUDED", RewardNotificationStatus.TO_SEND, dayBefore);
        // useCases for notification detail API:
        // 9: useCase EXPORTED (no CRO and transferDate)
        addFindInitiatives2NotifyUseCase(INITIATIVEIDDETAIL, RewardNotificationStatus.EXPORTED, dayBefore, null);
        // 10: useCase COMPLETED_OK with expected CRO
        addFindInitiatives2NotifyUseCase(INITIATIVEIDDETAIL, RewardNotificationStatus.COMPLETED_OK, dayBefore, CRO);
        // 11: useCase COMPLETED_OK with wrong CRO
        addFindInitiatives2NotifyUseCase(INITIATIVEIDDETAIL, RewardNotificationStatus.COMPLETED_OK, dayBefore, null);
        // 12: useCase COMPLETED_KO (no CRO)
        addFindInitiatives2NotifyUseCase(INITIATIVEIDDETAIL, RewardNotificationStatus.COMPLETED_KO, dayBefore, null);

        repository.saveAll(testData).collectList().block();

        NOTIFICATIONS_WITH_EXPORTID = List.of(testData.get(10), testData.get(11), testData.get(12), testData.get(5), testData.get(7), testData.get(9));
        NOTIFICATIONS_DETAIL = List.of(testData.get(10), testData.get(11), testData.get(12), testData.get(9));
    }

    private void addFindInitiatives2NotifyUseCase(RewardNotificationStatus status, Integer dayBefore) {
        addFindInitiatives2NotifyUseCase(null, status, dayBefore, null);
    }

    private void addFindInitiatives2NotifyUseCase(String initiativeId, RewardNotificationStatus status, Integer dayBefore) {
        int useCase = testData.size();

        testData.add(RewardsNotification.builder()
                .id("ID%d".formatted(useCase))
                .initiativeId(ObjectUtils.firstNonNull(initiativeId, "INITIATIVEID%d".formatted(useCase)))
                .organizationId("ORGANIZATIONID")
                .status(status)
                .notificationDate(dayBefore==null?null: NOTIFICATION_DATE.minusDays(dayBefore))
                .exportId(NotificationConstants.REWARD_NOTIFICATION_EXPOSED_STATUS.contains(status) ? "EXPORTID" : null)
                .build());
    }

    private void addFindInitiatives2NotifyUseCase(String initiativeId, RewardNotificationStatus status, Integer dayBefore, String cro) {
        int useCase = testData.size();

        testData.add(RewardsNotification.builder()
                .id("ID%d".formatted(useCase))
                .initiativeId(ObjectUtils.firstNonNull(initiativeId, "INITIATIVEID%d".formatted(useCase)))
                .organizationId("ORGANIZATIONID")
                .status(status)
                .notificationDate(dayBefore==null?null: NOTIFICATION_DATE.minusDays(dayBefore))
                .exportId(NotificationConstants.REWARD_NOTIFICATION_EXPOSED_STATUS.contains(status) ? "EXPORTID" : null)
                .cro(getCro(status, cro, useCase))
                .executionDate(STATUS_WITH_EXECUTION_DATE.contains(status) ? NOTIFICATION_DATE : null)
                .build());
    }

    private String getCro(RewardNotificationStatus status, String cro, int useCase) {
        return RewardNotificationStatus.COMPLETED_OK.equals(status)
                ? ObjectUtils.firstNonNull(cro, "CRO%d".formatted(useCase))
                : null;
    }

    @AfterEach
    void clearData(){
        repository.deleteAllById(testData.stream().map(RewardsNotification::getId).toList()).block();
    }

    @Test
    void findInitiatives2NotifyTest(){
        Assertions.assertEquals(
                List.of("INITIATIVEID3", "INITIATIVEIDNOTIFIEDTWICE"),
                repository.findInitiatives2Notify(List.of("INITIATIVEEXCLUDED"), NOTIFICATION_DATE).sort().collectList().block()
        );
    }

    @Test
    void findRewards2NotifyTest(){
        Assertions.assertEquals(
                List.of(testData.get(4), testData.get(6)),
                repository.findRewards2Notify("INITIATIVEIDNOTIFIEDTWICE", NOTIFICATION_DATE).sort(Comparator.comparing(RewardsNotification::getId)).collectList().block()
        );
    }

    @Test
    void findExportRewardsTest(){
        Assertions.assertEquals(
                NOTIFICATIONS_WITH_EXPORTID,
                repository.findExportRewards("EXPORTID").sort(Comparator.comparing(RewardsNotification::getId)).collectList().block()
        );
    }

    @Test
    void testSaveIfNotExists(){
        RewardsNotification updated = testData.get(0).toBuilder().build();
        updated.setCro("UPDATED");
        RewardsNotification result = repository.saveIfNotExists(updated).block();
        Assertions.assertNull(result);

        RewardsNotification fetched = repository.findById(updated.getId()).block();
        Assertions.assertNotNull(fetched);
        Assertions.assertEquals(testData.get(0).getCro(), fetched.getCro());

    }

    @Test
    void testFindAll() {
        // null pageable
        Assertions.assertEquals(
                NOTIFICATIONS_DETAIL,
                repository.findAll("EXPORTID", "ORGANIZATIONID", INITIATIVEIDDETAIL, EMPTY_FILTERS, null).sort(Comparator.comparing(RewardsNotification::getId)).collectList().block()
        );

        // no filters
        Assertions.assertEquals(
                NOTIFICATIONS_DETAIL,
                repository.findAll("EXPORTID", "ORGANIZATIONID", INITIATIVEIDDETAIL, EMPTY_FILTERS, DEFAULT_PAGEABLE).sort(Comparator.comparing(RewardsNotification::getId)).collectList().block()
        );

        // filters
        Assertions.assertEquals(
                List.of(testData.get(10)),
                repository.findAll("EXPORTID", "ORGANIZATIONID", INITIATIVEIDDETAIL, new ExportDetailFilter("COMPLETED_OK", CRO), DEFAULT_PAGEABLE).collectList().block()
        );

        // filters with status not exposed
        Assertions.assertEquals(
                Collections.emptyList(),
                repository.findAll("EXPORTID", "ORGANIZATIONID", "INITIATIVEID", new ExportDetailFilter("TO_SEND", null), DEFAULT_PAGEABLE).collectList().block()
        );
    }

    @Test
    void testCountAll() {
        // no filters
        Assertions.assertEquals(
                NOTIFICATIONS_DETAIL.size(),
                repository.countAll("EXPORTID", "ORGANIZATIONID", INITIATIVEIDDETAIL, EMPTY_FILTERS).block()
        );

        // filters with status not exposed
        Assertions.assertEquals(
                0L,
                repository.countAll("EXPORTID", "ORGANIZATIONID", "INITIATIVEID", new ExportDetailFilter("TO_SEND", null)).block()
        );
    }
}
