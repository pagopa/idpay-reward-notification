package it.gov.pagopa.reward.notification.utils;

import it.gov.pagopa.common.utils.AuditLogger;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j(topic = "AUDIT")
public class AuditUtilities {

    private static final String CEF = String.format("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=RewardNotification dstip=%s", AuditLogger.SRCIP);
    private static final String CEF_PATTERN = CEF + " msg={} cs1Label=initiativeId cs1={} cs2Label=organizationId cs2={}";
    private static final String CEF_USER_PATTERN = CEF_PATTERN + " suser={}";
    private static final String CEF_PATTERN_FILE = CEF_PATTERN + " cs3Label=fileName cs3={}";

    public void logUploadFile(String initiativeId, String organizationId, String fileName) {
        AuditLogger.logAuditString(
                CEF_PATTERN_FILE,
                "Uploading refunds file.", initiativeId, organizationId, fileName
        );
    }

    public void logDownloadFile(String initiativeId, String organizationId) {
        AuditLogger.logAuditString(
                CEF_PATTERN,
                "Downloading refunds file.", initiativeId, organizationId
        );
    }

    public void logGetExportsPaged(String initiativeId, String organizationId) {
        AuditLogger.logAuditString(
                CEF_PATTERN,
                "Export detail page about refunds.", initiativeId, organizationId
        );
    }

    public void logSuspension(String initiativeId, String organizationId, String userId) {
        AuditLogger.logAuditString(
                CEF_USER_PATTERN,
                "User suspended", initiativeId, organizationId, userId
        );
    }

    public void logSuspensionKO(String initiativeId, String organizationId, String userId) {
        AuditLogger.logAuditString(
                CEF_USER_PATTERN,
                "User suspension failed", initiativeId, organizationId, userId
        );
    }

    public void logReadmission(String initiativeId, String organizationId, String userId) {
        AuditLogger.logAuditString(
                CEF_USER_PATTERN,
                "User readmitted", initiativeId, organizationId, userId
        );
    }

    public void logReadmissionKO(String initiativeId, String organizationId, String userId) {
        AuditLogger.logAuditString(
                CEF_USER_PATTERN,
                "User readmission failed", initiativeId, organizationId, userId
        );
    }
}