package it.gov.pagopa.reward.notification.repository;

import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import org.apache.commons.lang3.ObjectUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class RewardsNotificationRepositoryExtendedTest extends BaseIntegrationTest {

    public static final LocalDate NOTIFICATION_DATE = LocalDate.now();
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

        repository.saveAll(testData).collectList().block();
    }

    private void addFindInitiatives2NotifyUseCase(RewardNotificationStatus status, Integer dayBefore) {
        addFindInitiatives2NotifyUseCase(null, status, dayBefore);
    }
    private void addFindInitiatives2NotifyUseCase(String initiativeId, RewardNotificationStatus status, Integer dayBefore) {
        int useCase = testData.size();

        testData.add(RewardsNotification.builder()
                .id("ID%d".formatted(useCase))
                .initiativeId(ObjectUtils.firstNonNull(initiativeId, "INITIATIVEID%d".formatted(useCase)))
                .organizationId("ORGANIZATIONID")
                .status(status)
                .notificationDate(dayBefore==null?null: NOTIFICATION_DATE.minusDays(dayBefore))
                .exportId(RewardNotificationStatus.EXPORTED.equals(status) ? "EXPORTID%d".formatted(useCase) : null)
                .build());
    }

    @AfterEach
    void clearData(){
        repository.deleteAllById(testData.stream().map(RewardsNotification::getId).toList()).block();
    }

    @Test
    void findInitiatives2NotifyTest(){
        Assertions.assertEquals(
                List.of("INITIATIVEID3", "INITIATIVEIDNOTIFIEDTWICE"),
                repository.findInitiatives2Notify(List.of("INITIATIVEEXCLUDED")).sort().collectList().block()
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
                List.of(testData.get(5)),
                repository.findExportRewards("EXPORTID5").collectList().block()
        );
    }
}
