package it.gov.pagopa.reward.notification.utils;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
@AllArgsConstructor
@Slf4j(topic = "AUDIT")
public class AuditUtilities {
    public static final String SRCIP;

    static {
        String srcIp;
        try {
            srcIp = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.error("Cannot determine the ip of the current host", e);
            srcIp="UNKNOWN";
        }
        SRCIP = srcIp;
    }

    private static final String CEF = String.format("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=RewardNotification dstip=%s", SRCIP);
    private static final String CEF_PATTERN = CEF + " msg={} cs1Label=initiativeId cs1={} cs2Label=organizationId cs2={}";
    private static final String CEF_USER_PATTERN = CEF_PATTERN + " suser={}";
    private static final String CEF_PATTERN_FILE = CEF_PATTERN + " cs3Label=fileName cs3={}";

    private void logAuditString(String pattern, String... parameters) {
        log.info(pattern, (Object[]) parameters);
    }

    public void logUploadFile(String initiativeId, String organizationId, String fileName) {
        logAuditString(
                CEF_PATTERN_FILE,
                "Uploading refunds file.", initiativeId, organizationId, fileName
        );
    }

    public void logDownloadFile(String initiativeId, String organizationId) {
        logAuditString(
                CEF_PATTERN,
                "Downloading refunds file.", initiativeId, organizationId
        );
    }

    public void logGetExportsPaged(String initiativeId, String organizationId) {
        logAuditString(
                CEF_PATTERN,
                "Export detail page about refunds.", initiativeId, organizationId
        );
    }

    public void logSuspension(String initiativeId, String organizationId, String userId) {
        logAuditString(
                CEF_USER_PATTERN,
                "User suspended", initiativeId, organizationId, userId
        );
    }

    public void logSuspensionKO(String initiativeId, String organizationId, String userId) {
        logAuditString(
                CEF_USER_PATTERN,
                "User suspension failed", initiativeId, organizationId, userId
        );
    }

    public void logReadmission(String initiativeId, String organizationId, String userId) {
        logAuditString(
                CEF_USER_PATTERN,
                "User readmitted", initiativeId, organizationId, userId
        );
    }

    public void logReadmissionKO(String initiativeId, String organizationId, String userId) {
        logAuditString(
                CEF_USER_PATTERN,
                "User readmission failed", initiativeId, organizationId, userId
        );
    }
}