package it.gov.pagopa.reward.notification.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import it.gov.pagopa.reward.notification.exception.ClientException;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AuditUtilities {
    private static final String SRCIP;

    static {
        try {
            SRCIP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new ClientException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private static final String CEF = String.format("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=RewardNotification dstip=%s", SRCIP);
    private static final String MSG = " msg=";
    private static final String INITIATIVE_ID = "cs1Label=initiativeId cs1=";
    private static final String ORGANIZATION_ID = "cs2Label=organizationId cs2=";
    private static final String FILE_NAME = "cs3Label=fileName cs3=";

    final Logger logger = Logger.getLogger("AUDIT");

    private String buildLog(String eventLog, String initiativeId, String organizationId) {
        return CEF + MSG + eventLog + " " + INITIATIVE_ID + initiativeId + " " + ORGANIZATION_ID + organizationId;
    }

    public void logUploadFile(String initiativeId, String organizationId, String fileName) {
        String testLog = this.buildLog("Uploading refunds file ", initiativeId, organizationId) + " " + FILE_NAME + fileName;
        logger.info(testLog);
    }

    public void logDownloadFile(String initiativeId, String organizationId) {
        String testLog = this.buildLog("Downloading refunds file ", initiativeId, organizationId);
        logger.info(testLog);
    }

    public void logGetExportsPaged(String initiativeId, String organizationId) {
        String testLog = this.buildLog("Export detail page about refunds ", initiativeId, organizationId);
        logger.info(testLog);
    }
    }