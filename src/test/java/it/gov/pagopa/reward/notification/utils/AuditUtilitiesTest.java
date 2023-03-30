package it.gov.pagopa.reward.notification.utils;

import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class AuditUtilitiesTest {
    private static final String INITIATIVE_ID = "TEST_INITIATIVE_ID";
    private static final String ORGANIZATION_ID = "TEST_ORGANIZATION_ID";
    private static final String USER_ID = "TEST_USER_ID";
    private static final String FILE_NAME = "TEST_FILE_NAME";

    private final AuditUtilities auditUtilities = new AuditUtilities();
    private MemoryAppender memoryAppender;

    @BeforeEach
    public void setup() {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("AUDIT");
        memoryAppender = new MemoryAppender();
        memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(ch.qos.logback.classic.Level.INFO);
        logger.addAppender(memoryAppender);
        memoryAppender.start();
    }

    @Test
    void logUploadFile_ok(){
        auditUtilities.logUploadFile(INITIATIVE_ID,ORGANIZATION_ID,FILE_NAME);

        Assertions.assertEquals(
                ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=RewardNotification dstip=%s msg=Uploading refunds file." +
                        " cs1Label=initiativeId cs1=%s cs2Label=organizationId cs2=%s cs3Label=fileName cs3=%s")
                        .formatted(
                                AuditUtilities.SRCIP,
                                INITIATIVE_ID,
                                ORGANIZATION_ID,
                                FILE_NAME
                        ),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }

    @Test
    void logDownloadFile_ok(){
        auditUtilities.logDownloadFile(INITIATIVE_ID,ORGANIZATION_ID);

        Assertions.assertEquals(
                ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=RewardNotification dstip=%s msg=Downloading refunds file." +
                        " cs1Label=initiativeId cs1=%s cs2Label=organizationId cs2=%s")
                        .formatted(
                                AuditUtilities.SRCIP,
                                INITIATIVE_ID,
                                ORGANIZATION_ID
                        ),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }
    @Test
    void logGetExportsPaged_ok(){
        auditUtilities.logGetExportsPaged(INITIATIVE_ID,ORGANIZATION_ID);

        Assertions.assertEquals(
                ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=RewardNotification dstip=%s msg=Export detail page about refunds." +
                        " cs1Label=initiativeId cs1=%s cs2Label=organizationId cs2=%s")
                        .formatted(
                                AuditUtilities.SRCIP,
                                INITIATIVE_ID,
                                ORGANIZATION_ID
                        ),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }

    @Test
    void logSuspension() {
        auditUtilities.logSuspension(INITIATIVE_ID,ORGANIZATION_ID,USER_ID);

        Assertions.assertEquals(
                ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=RewardNotification dstip=%s msg=User suspended" +
                        " cs1Label=initiativeId cs1=%s cs2Label=organizationId cs2=%s suser=%s")
                        .formatted(
                                AuditUtilities.SRCIP,
                                INITIATIVE_ID,
                                ORGANIZATION_ID,
                                USER_ID
                        ),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }

    @Test
    void logSuspensionKO() {
        auditUtilities.logSuspensionKO(INITIATIVE_ID,ORGANIZATION_ID,USER_ID);

        Assertions.assertEquals(
                ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=RewardNotification dstip=%s msg=User suspension failed" +
                        " cs1Label=initiativeId cs1=%s cs2Label=organizationId cs2=%s suser=%s")
                        .formatted(
                                AuditUtilities.SRCIP,
                                INITIATIVE_ID,
                                ORGANIZATION_ID,
                                USER_ID
                        ),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }
}
