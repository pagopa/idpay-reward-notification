package it.gov.pagopa.reward.notification.service.utils;

import it.gov.pagopa.reward.notification.enums.ExportStatus;

import java.util.Arrays;
import java.util.List;

public final class ExportConstants {

    private ExportConstants() {}

    public static final List<ExportStatus> EXPORT_EXPOSED_STATUSES = Arrays.asList(
            ExportStatus.EXPORTED,
            ExportStatus.PARTIAL,
            ExportStatus.COMPLETE
    );
}
