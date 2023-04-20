package it.gov.pagopa.reward.notification.service.exports.detail;

import it.gov.pagopa.reward.notification.dto.controller.detail.*;
import it.gov.pagopa.reward.notification.dto.mapper.detail.PageImpl2ExportPageDTOMapper;
import it.gov.pagopa.reward.notification.dto.mapper.detail.RewardOrganizationExport2ExportSummaryDTOMapper;
import it.gov.pagopa.reward.notification.dto.mapper.detail.RewardsNotification2DTOMapper;
import it.gov.pagopa.reward.notification.dto.mapper.detail.RewardsNotification2DetailDTOMapper;
import it.gov.pagopa.reward.notification.enums.RewardNotificationStatus;
import it.gov.pagopa.reward.notification.enums.RewardOrganizationExportStatus;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.model.RewardsNotification;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationExportsRepository;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import it.gov.pagopa.reward.notification.test.fakers.RewardOrganizationExportsFaker;
import it.gov.pagopa.reward.notification.test.fakers.RewardsNotificationFaker;
import it.gov.pagopa.reward.notification.utils.Utils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

@ExtendWith(MockitoExtension.class)
class ExportDetailServiceImplTest {

    private static final int NOTIFICATION_LIST_SIZE = 3;
    private static final LocalDate DATE = LocalDate.of(2001, 2, 4);
    public static final ExportDetailFilter EMPTY_FILTERS = new ExportDetailFilter();
    public static final PageRequest DEFAULT_PAGEABLE = PageRequest.of(0, 10);

    @Mock
    private RewardOrganizationExportsRepository exportsRepositoryMock;
    @Mock
    private RewardsNotificationRepository notificationRepositoryMock;

    private final RewardOrganizationExport2ExportSummaryDTOMapper summaryMapper = new RewardOrganizationExport2ExportSummaryDTOMapper();
    private final RewardsNotification2DTOMapper exportDetailMapper = new RewardsNotification2DTOMapper();
    private final PageImpl2ExportPageDTOMapper pageMapper = new PageImpl2ExportPageDTOMapper();
    private final RewardsNotification2DetailDTOMapper notificationDetailMapper = new RewardsNotification2DetailDTOMapper();

    private ExportDetailService service;

    @BeforeEach
    void setup() {
        service = new ExportDetailServiceImpl(exportsRepositoryMock, notificationRepositoryMock, summaryMapper, exportDetailMapper, pageMapper, notificationDetailMapper);
    }

    @Test
    void testGetExport() {
        RewardOrganizationExport export = RewardOrganizationExportsFaker.mockInstance(1);

        Mockito.when(exportsRepositoryMock.findByIdAndOrganizationIdAndInitiativeId(export.getId(), export.getOrganizationId(), export.getInitiativeId()))
                .thenReturn(Mono.just(export));

        ExportSummaryDTO result = service.getExport(export.getId(), export.getOrganizationId(), export.getInitiativeId()).block();

        ExportSummaryDTO expectedSummary = ExportSummaryDTO.builder()
                .createDate(DATE)
                .totalAmount(BigDecimal.valueOf(100_00, 2))
                .totalRefundedAmount(BigDecimal.valueOf(100_00, 2))
                .totalRefunds(2L)
                .successPercentage("10")
                .status(RewardOrganizationExportStatus.EXPORTED)
                .build();
        Assertions.assertEquals(expectedSummary, result);
    }

    @Test
    void testGetExportNotifications() {
        RewardOrganizationExport export = RewardOrganizationExportsFaker.mockInstanceBuilder(0)
                .initiativeId("INITIATIVEID")
                .build();

        List<RewardsNotification> notificationList = IntStream.range(0, NOTIFICATION_LIST_SIZE)
                .mapToObj(this::rewardsNotificationMockInstance)
                .toList();

        Mockito.when(exportsRepositoryMock.findByIdAndOrganizationIdAndInitiativeId(export.getId(), export.getOrganizationId(), export.getInitiativeId()))
                .thenReturn(Mono.just(export));
        Mockito.when(notificationRepositoryMock.findAll(export.getId(), export.getOrganizationId(), export.getInitiativeId(), EMPTY_FILTERS, DEFAULT_PAGEABLE))
                .thenReturn(Flux.fromIterable(notificationList));

        List<RewardNotificationDTO> result =
                service.getExportNotifications(export.getId(), export.getOrganizationId(), export.getInitiativeId(), EMPTY_FILTERS, DEFAULT_PAGEABLE)
                        .collectList().block();

        List<RewardNotificationDTO> expectedList = IntStream.range(0, NOTIFICATION_LIST_SIZE)
                .mapToObj(this::rewardNotificationDTOMockInstance)
                .toList();
        Assertions.assertEquals(expectedList, result);
    }

    @Test
    void testGetExportNotificationsPaged() {
        Pageable pageable = PageRequest.of(0,12);

        RewardOrganizationExport export = RewardOrganizationExportsFaker.mockInstanceBuilder(0)
                .initiativeId("INITIATIVEID")
                .build();

        List<RewardsNotification> notificationList = IntStream.range(0, NOTIFICATION_LIST_SIZE)
                .mapToObj(this::rewardsNotificationMockInstance)
                .toList();

        Mockito.when(exportsRepositoryMock.findByIdAndOrganizationIdAndInitiativeId(export.getId(), export.getOrganizationId(), export.getInitiativeId()))
                .thenReturn(Mono.just(export));
        Mockito.when(notificationRepositoryMock.findAll(export.getId(), export.getOrganizationId(), export.getInitiativeId(), EMPTY_FILTERS, pageable))
                .thenReturn(Flux.fromIterable(notificationList));
        Mockito.when(notificationRepositoryMock.countAll(export.getId(), export.getOrganizationId(), export.getInitiativeId(), EMPTY_FILTERS))
                .thenReturn(Mono.just((long) NOTIFICATION_LIST_SIZE));

        ExportContentPageDTO result = service.getExportNotificationsPaged(export.getId(), export.getOrganizationId(), export.getInitiativeId(), EMPTY_FILTERS, pageable).block();

        List<RewardNotificationDTO> expectedContent = IntStream.range(0, NOTIFICATION_LIST_SIZE)
                .mapToObj(this::rewardNotificationDTOMockInstance)
                .toList();
        ExportContentPageDTO expectedPage = ExportContentPageDTO.builder()
                .content(expectedContent)
                .pageNo(0)
                .pageSize(12)
                .totalPages(1)
                .totalElements(NOTIFICATION_LIST_SIZE)
                .build();
        Assertions.assertEquals(expectedPage, result);
    }

    @Test
    void testGetEmptyPage() {
        ExportContentPageDTO result = service.getExportNotificationEmptyPage(DEFAULT_PAGEABLE).block();

        ExportContentPageDTO expected = ExportContentPageDTO.builder()
                .content(Collections.emptyList())
                .pageNo(0)
                .pageSize(10)
                .totalElements(0)
                .totalPages(0)
                .build();
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testGetRewardNotification() {
        int bias = 1;
        RewardsNotification notification = rewardsNotificationMockInstance(bias);

        Mockito.when(notificationRepositoryMock.findByExternalIdAndOrganizationIdAndInitiativeId(notification.getExternalId(), notification.getOrganizationId(), notification.getInitiativeId()))
                .thenReturn(Mono.just(notification));

        RewardNotificationDetailDTO result = service.getRewardNotification(notification.getExternalId(), notification.getOrganizationId(), notification.getInitiativeId()).block();

        RewardNotificationDetailDTO expected = RewardNotificationDetailDTO.builder()
                .id("USERID%s_INITIATIVEID_%s".formatted(bias, DATE.format(Utils.FORMATTER_DATE)))
                .externalId("EXTERNALID%s".formatted(bias))
                .userId("USERID%s".formatted(bias))
                .iban("IBAN%s".formatted(bias))
                .amount(BigDecimal.valueOf(getCents(bias), 2))
                .startDate(DATE)
                .endDate(DATE)
                .status(RewardNotificationStatus.EXPORTED)
                .refundType(Utils.RefundType.ORDINARY)
                .cro("CRO%s".formatted(bias))
                .transferDate(DATE)
                .userNotificationDate(DATE)
                .build();
        Assertions.assertEquals(expected, result);
    }

    private RewardsNotification rewardsNotificationMockInstance(int bias) {
        return RewardsNotificationFaker.mockInstance(bias, "INITIATIVEID", DATE).toBuilder()
                .id("USERID%s_INITIATIVEID_%s".formatted(bias, DATE.format(Utils.FORMATTER_DATE)))
                .externalId("EXTERNALID%s".formatted(bias))
                .iban("IBAN%s".formatted(bias))
                .rewardCents(getCents(bias))
                .status(RewardNotificationStatus.EXPORTED)
                .cro("CRO%s".formatted(bias))
                .startDepositDate(DATE)
                .executionDate(DATE)
                .feedbackElaborationDate(DATE.atTime(1, 0))
                .build();
    }

    private static long getCents(int bias) {
        return (long) (bias > 0
                ? (bias + (1.47 * bias)) * 100L
                : 1.47 * 100L);
    }

    private RewardNotificationDTO rewardNotificationDTOMockInstance(int bias) {
        return RewardNotificationDTO.builder()
                .eventId("EXTERNALID%s".formatted(bias))
                .iban("IBAN%s".formatted(bias))
                .amount(BigDecimal.valueOf(getCents(bias), 2))
                .status(RewardNotificationStatus.EXPORTED)
                .build();
    }
}