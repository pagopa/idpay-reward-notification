package it.gov.pagopa.reward.notification.service.exports.detail;

import it.gov.pagopa.reward.notification.dto.mapper.detail.PageImpl2ExportPageDTOMapper;
import it.gov.pagopa.reward.notification.dto.mapper.detail.RewardOrganizationExport2ExportSummaryDTOMapper;
import it.gov.pagopa.reward.notification.dto.mapper.detail.RewardsNotification2ExportDetailDTOMapper;
import it.gov.pagopa.reward.notification.dto.mapper.detail.RewardsNotification2DetailDTOMapper;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationExportsRepository;
import it.gov.pagopa.reward.notification.repository.RewardsNotificationRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExportDetailServiceImplTest {

    @Mock
    private RewardOrganizationExportsRepository exportsRepositoryMock;
    @Mock
    private RewardsNotificationRepository notificationRepositoryMock;

    private final RewardOrganizationExport2ExportSummaryDTOMapper summaryMapper = new RewardOrganizationExport2ExportSummaryDTOMapper();
    private final RewardsNotification2ExportDetailDTOMapper exportDetailMapper = new RewardsNotification2ExportDetailDTOMapper();
    private final PageImpl2ExportPageDTOMapper pageMapper = new PageImpl2ExportPageDTOMapper();
    private final RewardsNotification2DetailDTOMapper refundMapper = new RewardsNotification2DetailDTOMapper();

    @Test
    void test() {
        boolean tmp = true;
        Assertions.assertTrue(tmp);
    }
}