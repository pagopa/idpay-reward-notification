package it.gov.pagopa.reward.notification.utils;

import it.gov.pagopa.reward.notification.enums.RewardOrganizationImportStatus;

import java.util.Arrays;
import java.util.List;

public class EmailNotificationConstants {

    private EmailNotificationConstants() {}

    //region imports
    public static final List<RewardOrganizationImportStatus> IMPORT_ELABORATED_STATUS_LIST = Arrays.asList(
            RewardOrganizationImportStatus.COMPLETE,
            RewardOrganizationImportStatus.WARN);
    public static final String ELABORATED_IMPORT_TEMPLATE_NAME = "Email_EsitiFile";

    public static final String ELABORATED_IMPORT_SUBJECT = "Esiti elaborati con successo";
    //endregion
}
