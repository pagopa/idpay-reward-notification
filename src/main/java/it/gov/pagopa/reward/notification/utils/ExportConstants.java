package it.gov.pagopa.reward.notification.utils;

import it.gov.pagopa.reward.notification.enums.RewardOrganizationExportStatus;

import java.util.Arrays;
import java.util.List;

public final class ExportConstants {

    private ExportConstants() {}

    public static final List<RewardOrganizationExportStatus> EXPORT_EXPOSED_STATUSES = Arrays.asList(
            RewardOrganizationExportStatus.EXPORTED,
            RewardOrganizationExportStatus.PARTIAL,
            RewardOrganizationExportStatus.COMPLETE
    );
}
